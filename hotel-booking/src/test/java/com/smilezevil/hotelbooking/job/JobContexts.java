package com.smilezevil.hotelbooking.job;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Date;
import java.util.Map;

final class JobContexts {

    private JobContexts() {
    }

    static JobExecutionContextImpl contextFor(Class<? extends Job> jobClass,
                                              Map<String, ?> jobDefaults,
                                              Map<String, ?> triggerData) {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity("testJob", "test")
                .usingJobData(new JobDataMap(jobDefaults))
                .storeDurably()
                .build();
        OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("testTrigger", "test")
                .usingJobData(new JobDataMap(triggerData))
                .startNow()
                .build();
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                jobDetail, trigger, null, false, new Date(), null, null, null);
        return new JobExecutionContextImpl(null, bundle, null);
    }
}