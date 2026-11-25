package com.smilezevil.hotelbooking.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentAuditorTest {

    private static final String SUBJECT = "1b88dd53-7fac-48e4-9fe0-5158bcd1a803";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void require_noAuthentication_throwsExplainingException() {
        assertThatThrownBy(CurrentAuditor::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Немає автентифікованого користувача")
                .hasMessageContaining("Фоновий код має сам встановити свою identity");
    }

    @Test
    void require_anonymousUser_throwsExplainingException() {
        signIn(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(CurrentAuditor::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Немає автентифікованого користувача");
    }

    @Test
    void require_notAuthenticatedToken_throwsExplainingException() {
        signIn(UsernamePasswordAuthenticationToken.unauthenticated("bob", "wrong-password"));

        assertThatThrownBy(CurrentAuditor::require)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void require_keycloakToken_returnsPreferredUsername() {
        signIn(keycloakToken(Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(SUBJECT)
                .claim("preferred_username", "bob")
                .build()));

        assertThat(CurrentAuditor.require()).isEqualTo("bob");
    }

    @Test
    void require_tokenWithoutPreferredUsername_returnsSubject() {
        signIn(keycloakToken(Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(SUBJECT)
                .build()));

        assertThat(CurrentAuditor.require()).isEqualTo(SUBJECT);
    }

    @Test
    void require_backgroundJobIdentity_returnsItsName() {
        signIn(UsernamePasswordAuthenticationToken.authenticated("batch:roomImport", null, List.of()));

        assertThat(CurrentAuditor.require()).isEqualTo("batch:roomImport");
    }

    @Test
    void auditorAware_authenticatedUser_givesSameUserToSpringData() {
        signIn(UsernamePasswordAuthenticationToken.authenticated("bob", null, List.of()));

        assertThat(new AuditingConfig().auditorAware().getCurrentAuditor()).contains("bob");
    }

    private static JwtAuthenticationToken keycloakToken(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
    }

    private static void signIn(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}