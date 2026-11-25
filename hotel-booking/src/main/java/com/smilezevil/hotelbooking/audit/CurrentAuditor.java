package com.smilezevil.hotelbooking.audit;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CurrentAuditor {

    private CurrentAuditor() {
    }

    public static String require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Немає автентифікованого користувача, тому невідомо, хто змінює дані. "
                    + "Фоновий код має сам встановити свою identity, як це робить імпорт CSV");
        }
        if (auth instanceof JwtAuthenticationToken jwt) {
            String username = jwt.getToken().getClaimAsString("preferred_username");
            if (username != null) {
                return username;
            }
        }
        return auth.getName();
    }
}