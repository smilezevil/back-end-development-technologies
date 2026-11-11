package com.smilezevil.hotelbooking.rates.grpc;

import com.smilezevil.exchangerate.api.ExchangeRatesGrpc;
import com.smilezevil.hotelbooking.rates.RatesProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rates.client", havingValue = "grpc")
@EnableConfigurationProperties(RatesProperties.class)
public class GrpcRatesConfig {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel ratesChannel(RatesProperties properties) {
        return ManagedChannelBuilder.forTarget(properties.grpc().target())
                .usePlaintext()
                .build();
    }

    @Bean
    public GrpcRatesAdapter grpcRatesAdapter(ManagedChannel ratesChannel, RatesProperties properties) {
        return new GrpcRatesAdapter(ExchangeRatesGrpc.newBlockingStub(ratesChannel), properties.grpc().deadline());
    }
}