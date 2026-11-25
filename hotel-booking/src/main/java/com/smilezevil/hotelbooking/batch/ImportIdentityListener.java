package com.smilezevil.hotelbooking.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class ImportIdentityListener implements JobExecutionListener {

    public static final String IMPORT_USER = "batch:roomImport";

    @Override
    public void beforeJob(JobExecution execution) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                IMPORT_USER, null, AuthorityUtils.createAuthorityList("ROLE_IMPORT")));
        SecurityContextHolder.setContext(context);
        log.info("ROOM IMPORT #{} started as {}, file {}", execution.getId(), IMPORT_USER,
                execution.getJobParameters().getString(RoomImportJobConfig.FILE_PARAMETER));
    }

    @Override
    public void afterJob(JobExecution execution) {
        SecurityContextHolder.clearContext();
        ImportCounts counts = ImportCounts.of(execution);
        log.info("ROOM IMPORT #{} finished with {}: read={}, write={}, filter={}, skip={}",
                execution.getId(), execution.getStatus(),
                counts.read(), counts.write(), counts.filter(), counts.skip());
    }
}