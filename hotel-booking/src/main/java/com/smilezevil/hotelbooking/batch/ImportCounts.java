package com.smilezevil.hotelbooking.batch;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import java.util.function.ToLongFunction;

public record ImportCounts(long read, long write, long filter, long skip) {

    public static ImportCounts of(JobExecution execution) {
        return new ImportCounts(
                sum(execution, StepExecution::getReadCount),
                sum(execution, StepExecution::getWriteCount),
                sum(execution, StepExecution::getFilterCount),
                sum(execution, StepExecution::getSkipCount));
    }

    private static long sum(JobExecution execution, ToLongFunction<StepExecution> counter) {
        return execution.getStepExecutions().stream().mapToLong(counter).sum();
    }
}