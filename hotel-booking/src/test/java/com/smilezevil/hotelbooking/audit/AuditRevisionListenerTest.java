package com.smilezevil.hotelbooking.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditRevisionListenerTest {

    private final AuditRevisionListener listener = new AuditRevisionListener();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newRevision_authenticatedUser_storesAuthorInRevision() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("bob", null, List.of()));
        AuditRevision revision = new AuditRevision();

        listener.newRevision(revision);

        assertThat(revision.getChangedBy()).isEqualTo("bob");
    }

    @Test
    void newRevision_noUser_throwsAndRevisionStaysWithoutAuthor() {
        AuditRevision revision = new AuditRevision();

        assertThatThrownBy(() -> listener.newRevision(revision))
                .isInstanceOf(IllegalStateException.class);
        assertThat(revision.getChangedBy()).isNull();
    }
}