package com.smilezevil.hotelbooking.resolver;

import com.smilezevil.hotelbooking.annotation.CurrentUsername;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUsernameArgumentResolverTest {

    private final CurrentUsernameArgumentResolver resolver = new CurrentUsernameArgumentResolver();

    static class SampleController {
        void withAnnotation(@CurrentUsername String username) {
        }

        void withoutAnnotation(String username) {
        }

        void withAnnotationButWrongType(@CurrentUsername Long userId) {
        }
    }


    @Test
    void supportsParameter_annotatedStringParameter_returnsTrue() throws Exception {
        MethodParameter parameter = parameterOf("withAnnotation", String.class);

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    void supportsParameter_parameterWithoutAnnotation_returnsFalse() throws Exception {
        MethodParameter parameter = parameterOf("withoutAnnotation", String.class);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    void supportsParameter_annotatedButNotString_returnsFalse() throws Exception {
        MethodParameter parameter = parameterOf("withAnnotationButWrongType", Long.class);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }


    @Test
    void resolveArgument_jwtUser_returnsPreferredUsername() throws Exception {
        Jwt jwt = jwtWithClaims(Map.of("sub", "user-id-123", "preferred_username", "alice"));
        NativeWebRequest request = requestWithUser(new JwtAuthenticationToken(jwt));

        Object result = resolver.resolveArgument(parameterOf("withAnnotation", String.class), null, request, null);

        assertThat(result).isInstanceOf(String.class).isEqualTo("alice");
    }

    @Test
    void resolveArgument_oidcUser_returnsPreferredUsername() throws Exception {
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T11:00:00Z"),
                Map.of("sub", "user-id-456", "preferred_username", "bob"));
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_user")), idToken);
        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak");
        NativeWebRequest request = requestWithUser(authentication);

        Object result = resolver.resolveArgument(parameterOf("withAnnotation", String.class), null, request, null);

        assertThat(result).isInstanceOf(String.class).isEqualTo("bob");
    }

    @Test
    void resolveArgument_jwtWithoutPreferredUsername_fallsBackToPrincipalName() throws Exception {
        Jwt jwt = jwtWithClaims(Map.of("sub", "user-id-123"));
        NativeWebRequest request = requestWithUser(new JwtAuthenticationToken(jwt));

        Object result = resolver.resolveArgument(parameterOf("withAnnotation", String.class), null, request, null);

        assertThat(result).isEqualTo("user-id-123");
    }

    @Test
    void resolveArgument_noAuthenticatedUser_returnsNull() throws Exception {
        NativeWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        Object result = resolver.resolveArgument(parameterOf("withAnnotation", String.class), null, request, null);

        assertThat(result).isNull();
    }


    private static MethodParameter parameterOf(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = SampleController.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .build();
    }

    private static NativeWebRequest requestWithUser(Principal user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(user);
        return new ServletWebRequest(request);
    }
}