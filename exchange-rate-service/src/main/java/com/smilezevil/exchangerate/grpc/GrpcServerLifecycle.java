package com.smilezevil.exchangerate.grpc;

import com.smilezevil.exchangerate.ExchangeRateService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "grpc.server.enabled", havingValue = "true")
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final ExchangeRateService exchangeRateService;
    private final int port;
    private Server server;

    public GrpcServerLifecycle(ExchangeRateService exchangeRateService,
                               @Value("${grpc.server.port}") int port) {
        this.exchangeRateService = exchangeRateService;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(new ExchangeRatesGrpcService(exchangeRateService))
                    .addService(ProtoReflectionService.newInstance())
                    .build()
                    .start();
            log.info("gRPC server started on port {}", server.getPort());
        } catch (IOException e) {
            throw new IllegalStateException("Could not start gRPC server on port " + port, e);
        }
    }

    @Override
    public void stop() {
        if (server == null) {
            return;
        }
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        }
        log.info("gRPC server stopped");
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }

    public int getPort() {
        return server.getPort();
    }
}