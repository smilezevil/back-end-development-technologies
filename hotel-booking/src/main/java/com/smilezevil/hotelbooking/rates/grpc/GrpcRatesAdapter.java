package com.smilezevil.hotelbooking.rates.grpc;

import com.smilezevil.exchangerate.api.ConvertReply;
import com.smilezevil.exchangerate.api.ConvertRequest;
import com.smilezevil.exchangerate.api.ExchangeRatesGrpc;
import com.smilezevil.exchangerate.api.RateReply;
import com.smilezevil.exchangerate.api.RateRequest;
import com.smilezevil.hotelbooking.exception.RateNotFoundException;
import com.smilezevil.hotelbooking.exception.RatesUnavailableException;
import com.smilezevil.hotelbooking.rates.ConvertedAmount;
import com.smilezevil.hotelbooking.rates.CurrencyConverter;
import com.smilezevil.hotelbooking.rates.ExchangeRate;
import com.smilezevil.hotelbooking.rates.RateLookup;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class GrpcRatesAdapter implements RateLookup, CurrencyConverter {

    private static final String VIA = "grpc";

    private final ExchangeRatesGrpc.ExchangeRatesBlockingStub stub;
    private final Duration deadline;

    public GrpcRatesAdapter(ExchangeRatesGrpc.ExchangeRatesBlockingStub stub, Duration deadline) {
        this.stub = stub;
        this.deadline = deadline;
    }

    @Override
    public ExchangeRate rateFor(String currency) {
        RateReply reply = call(currency, () -> withDeadline()
                .getRate(RateRequest.newBuilder().setCurrency(currency).build()));
        return new ExchangeRate(reply.getCurrency(), new BigDecimal(reply.getUahPerUnit()), VIA);
    }

    @Override
    public ConvertedAmount convert(BigDecimal amountUah, String currency) {
        ConvertReply reply = call(currency, () -> withDeadline()
                .convert(ConvertRequest.newBuilder()
                        .setCurrency(currency)
                        .setAmountUah(amountUah.toPlainString())
                        .build()));
        return new ConvertedAmount(new BigDecimal(reply.getAmountUah()), reply.getCurrency(),
                new BigDecimal(reply.getAmount()), VIA);
    }

    private ExchangeRatesGrpc.ExchangeRatesBlockingStub withDeadline() {
        return stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private <T> T call(String currency, Supplier<T> request) {
        try {
            return request.get();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new RateNotFoundException(currency);
            }
            throw new RatesUnavailableException("Сервіс курсів валют недоступний (grpc): " + e.getStatus().getCode(), e);
        }
    }
}