package com.smilezevil.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private AsyncTaskExecutor applicationTaskExecutor;


    @Test
    void context_startsWithoutOtherServices_usesOwnNameAndPort() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("notification-service");
        assertThat(environment.getProperty("server.port")).isEqualTo("8082");
    }

    @Test
    void applicationTaskExecutor_virtualThreadsEnabled_runsTasksOnVirtualThreads() throws Exception {
        Boolean virtual = applicationTaskExecutor.submit(() -> Thread.currentThread().isVirtual())
                .get(5, TimeUnit.SECONDS);

        assertThat(applicationTaskExecutor).isInstanceOf(SimpleAsyncTaskExecutor.class);
        assertThat(virtual).isTrue();
    }
}