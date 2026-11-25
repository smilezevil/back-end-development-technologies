package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.audit.CurrentAuditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportIdentityListenerTest {

    private final ImportIdentityListener listener = new ImportIdentityListener();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void beforeJob_importBecomesAuthorOfChanges() {
        listener.beforeJob(execution());

        assertThat(CurrentAuditor.require()).isEqualTo("batch:roomImport");
    }

    @Test
    void afterJob_identityIsRemovedFromThread() {
        JobExecution execution = execution();
        listener.beforeJob(execution);

        listener.afterJob(execution);

        assertThatThrownBy(CurrentAuditor::require).isInstanceOf(IllegalStateException.class);
    }

    private static JobExecution execution() {
        return new JobExecution(1L, new JobParametersBuilder()
                .addString(RoomImportJobConfig.FILE_PARAMETER, "rooms.csv")
                .toJobParameters());
    }
}