package com.smilezevil.hotelbooking.dto;

import com.smilezevil.hotelbooking.batch.ImportCounts;
import com.smilezevil.hotelbooking.batch.RoomImportJobConfig;
import org.springframework.batch.core.JobExecution;

import java.time.LocalDateTime;

public record ImportStatusDTO(Long executionId,
                              String file,
                              String status,
                              String exitCode,
                              LocalDateTime startTime,
                              LocalDateTime endTime,
                              long readCount,
                              long writeCount,
                              long filterCount,
                              long skipCount,
                              String error) {

    public static ImportStatusDTO from(JobExecution execution) {
        ImportCounts counts = ImportCounts.of(execution);
        return new ImportStatusDTO(
                execution.getId(),
                execution.getJobParameters().getString(RoomImportJobConfig.FILE_PARAMETER),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                execution.getStartTime(),
                execution.getEndTime(),
                counts.read(),
                counts.write(),
                counts.filter(),
                counts.skip(),
                firstError(execution));
    }

    private static String firstError(JobExecution execution) {
        return execution.getStepExecutions().stream()
                .map(step -> step.getExitStatus().getExitDescription())
                .filter(description -> description != null && !description.isBlank())
                .map(description -> description.lines().findFirst().orElse(description))
                .findFirst()
                .orElse(null);
    }
}