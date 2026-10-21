package com.smilezevil.hotelbooking.job;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import java.util.Set;
import java.util.TreeSet;

public final class JobParameters {

    private JobParameters() {
    }

    public static void requireOnlyKnownKeys(JobDataMap data, Set<String> knownKeys) throws JobExecutionException {
        Set<String> unknown = new TreeSet<>(data.keySet());
        unknown.removeAll(knownKeys);
        if (!unknown.isEmpty()) {
            throw new JobExecutionException("Unknown JobDataMap key(s) " + unknown
                    + ", expected only " + new TreeSet<>(knownKeys));
        }
    }

    public static int requiredNonNegativeInt(JobDataMap data, String key) throws JobExecutionException {
        if (!data.containsKey(key)) {
            throw new JobExecutionException("Missing required JobDataMap key '" + key + "'");
        }
        Object value = data.get(key);
        if (!(value instanceof Integer number)) {
            throw new JobExecutionException("JobDataMap key '" + key + "' must be an Integer but was '" + value + "'");
        }
        if (number < 0) {
            throw new JobExecutionException("JobDataMap key '" + key + "' must not be negative but was " + number);
        }
        return number;
    }
}