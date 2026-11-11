package com.smilezevil.hotelbooking.rates;

import com.smilezevil.hotelbooking.rates.grpc.GrpcRatesAdapter;
import com.smilezevil.hotelbooking.rates.grpc.GrpcRatesConfig;
import com.smilezevil.hotelbooking.rates.http.HttpRatesAdapter;
import com.smilezevil.hotelbooking.rates.http.HttpRatesConfig;
import com.smilezevil.hotelbooking.repository.BookingRepository;
import com.smilezevil.hotelbooking.repository.RoomRepository;
import com.smilezevil.hotelbooking.service.CurrencyPriceService;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RatesClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HttpRatesConfig.class, GrpcRatesConfig.class)
            .withBean(RoomRepository.class, () -> mock(RoomRepository.class))
            .withBean(BookingRepository.class, () -> mock(BookingRepository.class))
            .withBean(CurrencyPriceService.class)
            .withPropertyValues(
                    "rates.rest.url=http://localhost:8081",
                    "rates.rest.connect-timeout=1s",
                    "rates.rest.read-timeout=2s",
                    "rates.grpc.target=localhost:9081",
                    "rates.grpc.deadline=2s");

    @Test
    void httpValue_selectsHttpAdapterForBothPorts() {
        contextRunner.withPropertyValues("rates.client=http").run(context -> {
            assertThat(context).hasSingleBean(HttpRatesAdapter.class);
            assertThat(context).doesNotHaveBean(GrpcRatesAdapter.class);
            assertThat(context.getBean(RateLookup.class)).isInstanceOf(HttpRatesAdapter.class);
            assertThat(context.getBean(CurrencyConverter.class)).isInstanceOf(HttpRatesAdapter.class);
            assertThat(context.getBean(RatesProperties.class).rest().connectTimeout()).isEqualTo(Duration.ofSeconds(1));
            assertThat(context.getBean(RatesProperties.class).rest().readTimeout()).isEqualTo(Duration.ofSeconds(2));
        });
    }

    @Test
    void grpcValue_selectsGrpcAdapterForBothPorts() {
        contextRunner.withPropertyValues("rates.client=grpc").run(context -> {
            assertThat(context).hasSingleBean(GrpcRatesAdapter.class);
            assertThat(context).hasSingleBean(ManagedChannel.class);
            assertThat(context).doesNotHaveBean(HttpRatesAdapter.class);
            assertThat(context.getBean(RateLookup.class)).isInstanceOf(GrpcRatesAdapter.class);
            assertThat(context.getBean(CurrencyConverter.class)).isInstanceOf(GrpcRatesAdapter.class);
            assertThat(context.getBean(RatesProperties.class).grpc().deadline()).isEqualTo(Duration.ofSeconds(2));
        });
    }

    @Test
    void unknownValue_stopsApplicationOnStartup() {
        contextRunner.withPropertyValues("rates.client=soap").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                    .hasMessageContaining("CurrencyConverter");
        });
    }
}