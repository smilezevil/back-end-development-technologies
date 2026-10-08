package com.smilezevil.hotelbooking.aspect;

import com.smilezevil.hotelbooking.annotation.RetryOnFailure;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class RetryAspect {

    @Around("@annotation(retryOnFailure)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnFailure retryOnFailure) throws Throwable {
        int maxAttempts = Math.max(1, retryOnFailure.maxAttempts());
        String method = joinPoint.getSignature().toShortString();

        for (int attempt = 1; ; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (TransientDataAccessException ex) {
                if (attempt >= maxAttempts) {
                    log.error("[@RetryOnFailure] {} failed after {} attempts", method, attempt);
                    throw ex;
                }
                log.warn("[@RetryOnFailure] {} attempt {}/{} failed ({}), retrying...",
                        method, attempt, maxAttempts, ex.getClass().getSimpleName());
                pause(retryOnFailure.backoffMs());
            }
        }
    }

    private void pause(long millis) throws InterruptedException {
        if (millis > 0) {
            Thread.sleep(millis);
        }
    }
}