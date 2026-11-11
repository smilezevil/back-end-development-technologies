package com.smilezevil.hotelbooking.rates.grpc;

import com.smilezevil.exchangerate.api.ConvertReply;
import com.smilezevil.exchangerate.api.ConvertRequest;
import com.smilezevil.exchangerate.api.ExchangeRatesGrpc;
import com.smilezevil.exchangerate.api.RateReply;
import com.smilezevil.exchangerate.api.RateRequest;
import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.RatesUnavailableException;
import com.smilezevil.hotelbooking.rates.ConvertedAmount;
import com.smilezevil.hotelbooking.rates.ExchangeRate;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcRatesAdapterTest {

    private Server server;
    private ManagedChannel channel;
    private GrpcRatesAdapter adapter;

    @BeforeEach
    void startFakeServer() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new FakeExchangeRates())
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        adapter = new GrpcRatesAdapter(ExchangeRatesGrpc.newBlockingStub(channel), Duration.ofMillis(300));
    }

    @AfterEach
    void stopFakeServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void rateFor_knownCurrency_returnsRateViaGrpc() {
        ExchangeRate rate = adapter.rateFor("EUR");

        assertThat(rate.currency()).isEqualTo("EUR");
        assertThat(rate.uahPerUnit()).isEqualByComparingTo("45.00");
        assertThat(rate.via()).isEqualTo("grpc");
    }

    @Test
    void convert_knownCurrency_returnsConvertedAmount() {
        ConvertedAmount converted = adapter.convert(new BigDecimal("1200.00"), "USD");

        assertThat(converted.amountUah()).isEqualByComparingTo("1200.00");
        assertThat(converted.currency()).isEqualTo("USD");
        assertThat(converted.amount()).isEqualByComparingTo("28.92");
        assertThat(converted.via()).isEqualTo("grpc");
    }

    @Test
    void notFoundStatus_becomesRateNotFound() {
        assertThatThrownBy(() -> adapter.rateFor("XYZ"))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void otherStatus_becomesRatesUnavailable() {
        assertThatThrownBy(() -> adapter.rateFor("ERR"))
                .isInstanceOf(RatesUnavailableException.class)
                .hasMessageContaining("INTERNAL");
    }

    @Test
    void silentServer_isCutByDeadline() {
        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> adapter.rateFor("SLOW"))
                .isInstanceOf(RatesUnavailableException.class)
                .hasMessageContaining("DEADLINE_EXCEEDED");
        assertThat(System.currentTimeMillis() - start).isLessThan(1500);
    }

    @Test
    void serverGone_becomesRatesUnavailable() {
        server.shutdownNow();

        assertThatThrownBy(() -> adapter.rateFor("EUR"))
                .isInstanceOf(RatesUnavailableException.class)
                .hasMessageContaining("UNAVAILABLE");
    }

    private static class FakeExchangeRates extends ExchangeRatesGrpc.ExchangeRatesImplBase {

        @Override
        public void getRate(RateRequest request, StreamObserver<RateReply> responseObserver) {
            switch (request.getCurrency()) {
                case "EUR" -> {
                    responseObserver.onNext(RateReply.newBuilder().setCurrency("EUR").setUahPerUnit("45.00").build());
                    responseObserver.onCompleted();
                }
                case "SLOW" -> {
                }
                case "ERR" -> responseObserver.onError(Status.INTERNAL.asRuntimeException());
                default -> responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            }
        }

        @Override
        public void convert(ConvertRequest request, StreamObserver<ConvertReply> responseObserver) {
            if (!"USD".equals(request.getCurrency())) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
                return;
            }
            responseObserver.onNext(ConvertReply.newBuilder()
                    .setAmountUah(request.getAmountUah())
                    .setCurrency("USD")
                    .setAmount("28.92")
                    .build());
            responseObserver.onCompleted();
        }
    }
}