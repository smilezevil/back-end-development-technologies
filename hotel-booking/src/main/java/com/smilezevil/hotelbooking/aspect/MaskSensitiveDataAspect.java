package com.smilezevil.hotelbooking.aspect;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Aspect
@Component
public class MaskSensitiveDataAspect {

    @AfterReturning(
            pointcut = "@annotation(com.smilezevil.hotelbooking.annotation.MaskSensitiveData)",
            returning = "result")
    public void mask(JoinPoint joinPoint, Object result) {
        if (result instanceof GuestDTO guest) {
            maskGuest(guest);
        } else if (result instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof GuestDTO guest) {
                    maskGuest(guest);
                }
            }
        }
        log.info("[@MaskSensitiveData] personal data masked in {}",
                joinPoint.getSignature().toShortString());
    }

    private void maskGuest(GuestDTO guest) {
        guest.setEmail(maskEmail(guest.getEmail()));
        guest.setPhone(maskPhone(guest.getPhone()));
    }

    private String maskEmail(String email) {
        int at = (email == null) ? -1 : email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}