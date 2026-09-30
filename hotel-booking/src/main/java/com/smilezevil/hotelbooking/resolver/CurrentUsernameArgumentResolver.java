package com.smilezevil.hotelbooking.resolver;

import com.smilezevil.hotelbooking.annotation.CurrentUsername;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;

public class CurrentUsernameArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUsername.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Principal principal = webRequest.getUserPrincipal();
        if (principal == null) {
            return null;
        }

        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username != null) {
                return username;
            }
        }

        if (principal instanceof OAuth2AuthenticationToken oauth2Auth
                && oauth2Auth.getPrincipal() instanceof OidcUser oidcUser
                && oidcUser.getPreferredUsername() != null) {
            return oidcUser.getPreferredUsername();
        }

        return principal.getName();
    }
}