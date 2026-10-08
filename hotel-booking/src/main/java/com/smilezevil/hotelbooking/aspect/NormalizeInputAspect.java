package com.smilezevil.hotelbooking.aspect;

import com.smilezevil.hotelbooking.annotation.NormalizeInput;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Aspect
@Component
public class NormalizeInputAspect {

    private static final String DTO_PACKAGE = "com.smilezevil.hotelbooking.dto";

    @Before("@annotation(normalizeInput)")
    public void normalize(JoinPoint joinPoint, NormalizeInput normalizeInput) {
        List<String> lowercaseFields = Arrays.asList(normalizeInput.lowercase());

        for (Object arg : joinPoint.getArgs()) {
            if (arg == null || !arg.getClass().getPackageName().equals(DTO_PACKAGE)) {
                continue;
            }

            ReflectionUtils.doWithFields(arg.getClass(), field -> {
                ReflectionUtils.makeAccessible(field);
                String value = (String) field.get(arg);
                if (value == null) {
                    return;
                }

                String normalized = value.strip().replaceAll("[ \\t]+", " ");
                if (lowercaseFields.contains(field.getName())) {
                    normalized = normalized.toLowerCase(Locale.ROOT);
                }

                if (!normalized.equals(value)) {
                    field.set(arg, normalized);
                    log.info("[@NormalizeInput] {}.{} normalized in {}",
                            arg.getClass().getSimpleName(), field.getName(),
                            joinPoint.getSignature().toShortString());
                }
            }, field -> field.getType() == String.class && !Modifier.isStatic(field.getModifiers()));
        }
    }
}