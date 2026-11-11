package com.smilezevil.exchangerate.grpc;

import com.smilezevil.exchangerate.ExchangeRateService;
import com.smilezevil.exchangerate.api.ExchangeRatesGrpc;
import com.smilezevil.exchangerate.api.RateReply;
import com.smilezevil.exchangerate.api.RateRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcServerLifecycleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ExchangeRateService.class)
            .withUserConfiguration(GrpcServerLifecycle.class)
            .withPropertyValues("grpc.server.port=0");

    @Test
    void serverIsDisabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(GrpcServerLifecycle.class));
    }

    @Test
    void enabledServer_startsWithContext_answersAndStopsWithContext() {
        AtomicReference<GrpcServerLifecycle> lifecycleRef = new AtomicReference<>();

        contextRunner.withPropertyValues("grpc.server.enabled=true").run(context -> {
            GrpcServerLifecycle lifecycle = context.getBean(GrpcServerLifecycle.class);
            lifecycleRef.set(lifecycle);
            assertThat(lifecycle.isRunning()).isTrue();

            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", lifecycle.getPort())
                    .usePlaintext()
                    .build();
            try {
                RateReply reply = ExchangeRatesGrpc.newBlockingStub(channel)
                        .getRate(RateRequest.newBuilder().setCurrency("PLN").build());
                assertThat(reply.getUahPerUnit()).isEqualTo("10.40");
            } finally {
                channel.shutdownNow();
            }
        });

        assertThat(lifecycleRef.get().isRunning()).isFalse();
    }
}