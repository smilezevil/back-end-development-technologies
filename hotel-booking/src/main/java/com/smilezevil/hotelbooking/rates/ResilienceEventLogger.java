package com.smilezevil.hotelbooking.rates;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResilienceEventLogger {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventLogger.class);
    private static final String RATES = "rates";

    public ResilienceEventLogger(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        retryRegistry.retry(RATES).getEventPublisher()
                .onRetry(event -> log.warn("RETRY {}: call failed with {}, retry #{} in {} ms",
                        event.getName(),
                        event.getLastThrowable().getClass().getSimpleName(),
                        event.getNumberOfRetryAttempts(),
                        event.getWaitInterval().toMillis()))
                .onSuccess(event -> log.info("RETRY {}: succeeded after {} retries",
                        event.getName(), event.getNumberOfRetryAttempts()))
                .onError(event -> log.warn("RETRY {}: gave up after {} attempts, last error: {}",
                        event.getName(), event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()));

        circuitBreakerRegistry.circuitBreaker(RATES).getEventPublisher()
                .onStateTransition(event -> log.warn("CIRCUIT {}: {} -> {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }
}