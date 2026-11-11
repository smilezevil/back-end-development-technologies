package com.smilezevil.exchangerate.grpc;

import com.smilezevil.common.dto.ConversionResponse;
import com.smilezevil.common.dto.ExchangeRateResponse;
import com.smilezevil.exchangerate.ExchangeRateService;
import com.smilezevil.exchangerate.UnknownCurrencyException;
import com.smilezevil.exchangerate.api.ConvertReply;
import com.smilezevil.exchangerate.api.ConvertRequest;
import com.smilezevil.exchangerate.api.ExchangeRatesGrpc;
import com.smilezevil.exchangerate.api.RateReply;
import com.smilezevil.exchangerate.api.RateRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class ExchangeRatesGrpcService extends ExchangeRatesGrpc.ExchangeRatesImplBase {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRatesGrpcService.class);

    private final ExchangeRateService exchangeRateService;

    public ExchangeRatesGrpcService(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    public void getRate(RateRequest request, StreamObserver<RateReply> responseObserver) {
        log.info("gRPC rate requested for {}", request.getCurrency());
        try {
            ExchangeRateResponse rate = exchangeRateService.rateFor(request.getCurrency());
            responseObserver.onNext(RateReply.newBuilder()
                    .setCurrency(rate.currency())
                    .setUahPerUnit(rate.uahPerUnit().toPlainString())
                    .build());
            responseObserver.onCompleted();
        } catch (UnknownCurrencyException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void convert(ConvertRequest request, StreamObserver<ConvertReply> responseObserver) {
        log.info("gRPC conversion requested: {} UAH -> {}", request.getAmountUah(), request.getCurrency());
        try {
            ConversionResponse conversion = exchangeRateService.convertFromUah(
                    new BigDecimal(request.getAmountUah()), request.getCurrency());
            responseObserver.onNext(ConvertReply.newBuilder()
                    .setAmountUah(conversion.amountUah().toPlainString())
                    .setCurrency(conversion.currency())
                    .setAmount(conversion.amount().toPlainString())
                    .build());
            responseObserver.onCompleted();
        } catch (UnknownCurrencyException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}