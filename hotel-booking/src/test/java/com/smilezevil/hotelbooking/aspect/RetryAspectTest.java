package com.smilezevil.hotelbooking.aspect;

import com.smilezevil.hotelbooking.annotation.RetryOnFailure;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private final RetryAspect aspect = new RetryAspect();

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("HotelService.findAll()");
    }

    @Test
    void retry_firstAttemptSucceeds_callsProceedOnceAndReturnsResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn(List.of("Bukovina Palace"));

        Object result = aspect.retry(joinPoint, retryOnFailure(3));

        assertThat(result).isEqualTo(List.of("Bukovina Palace"));
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void retry_transientFailureThenSuccess_retriesAndReturnsResult() throws Throwable {
        when(joinPoint.proceed())
                .thenThrow(new QueryTimeoutException("timeout"))
                .thenThrow(new QueryTimeoutException("timeout"))
                .thenReturn(List.of("Bukovina Palace"));

        Object result = aspect.retry(joinPoint, retryOnFailure(3));

        assertThat(result).isEqualTo(List.of("Bukovina Palace"));
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void retry_transientFailureEveryTime_throwsAfterMaxAttempts() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> aspect.retry(joinPoint, retryOnFailure(3)))
                .isInstanceOf(QueryTimeoutException.class);
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void retry_nonTransientException_doesNotRetry() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Готель з ID 99 не знайдено"));

        assertThatThrownBy(() -> aspect.retry(joinPoint, retryOnFailure(3)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Готель з ID 99 не знайдено");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void retry_customMaxAttempts_isTakenFromAnnotation() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> aspect.retry(joinPoint, retryOnFailure(5)))
                .isInstanceOf(QueryTimeoutException.class);
        verify(joinPoint, times(5)).proceed();
    }


    private static RetryOnFailure retryOnFailure(int maxAttempts) {
        return new RetryOnFailure() {
            @Override
            public int maxAttempts() {
                return maxAttempts;
            }

            @Override
            public long backoffMs() {
                return 0;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return RetryOnFailure.class;
            }
        };
    }
}