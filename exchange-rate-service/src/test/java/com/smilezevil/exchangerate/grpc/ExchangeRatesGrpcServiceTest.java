package com.smilezevil.exchangerate.grpc;

import com.smilezevil.exchangerate.ExchangeRateService;
import com.smilezevil.exchangerate.api.ConvertReply;
import com.smilezevil.exchangerate.api.ConvertRequest;
import com.smilezevil.exchangerate.api.ExchangeRatesGrpc;
import com.smilezevil.exchangerate.api.RateReply;
import com.smilezevil.exchangerate.api.RateRequest;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRatesGrpcServiceTest {

    private Server server;
    private ManagedChannel channel;
    private ExchangeRatesGrpc.ExchangeRatesBlockingStub stub;

    @BeforeEach
    void startInProcessServer() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new ExchangeRatesGrpcService(new ExchangeRateService()))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = ExchangeRatesGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopInProcessServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void getRate_knownCurrency_returnsRate() {
        RateReply reply = stub.getRate(RateRequest.newBuilder().setCurrency("eur").build());

        assertThat(reply.getCurrency()).isEqualTo("EUR");
        assertThat(reply.getUahPerUnit()).isEqualTo("45.00");
    }

    @Test
    void getRate_unknownCurrency_returnsNotFound() {
        RateRequest request = RateRequest.newBuilder().setCurrency("XYZ").build();

        assertThatThrownBy(() -> stub.getRate(request))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void convert_knownCurrency_returnsConvertedAmount() {
        ConvertReply reply = stub.convert(ConvertRequest.newBuilder()
                .setCurrency("USD")
                .setAmountUah("1200")
                .build());

        assertThat(reply.getAmountUah()).isEqualTo("1200");
        assertThat(reply.getCurrency()).isEqualTo("USD");
        assertThat(reply.getAmount()).isEqualTo("28.92");
    }

    @Test
    void convert_unknownCurrency_returnsNotFound() {
        ConvertRequest request = ConvertRequest.newBuilder().setCurrency("XYZ").setAmountUah("100").build();

        assertThatThrownBy(() -> stub.convert(request))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void convert_negativeAmount_returnsInvalidArgument() {
        ConvertRequest request = ConvertRequest.newBuilder().setCurrency("USD").setAmountUah("-5").build();

        assertThatThrownBy(() -> stub.convert(request))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }
}