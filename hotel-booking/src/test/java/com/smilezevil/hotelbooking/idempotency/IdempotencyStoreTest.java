package com.smilezevil.hotelbooking.idempotency;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyStoreTest {

    private final IdempotencyStore store = new IdempotencyStore();

    @Test
    void newKey_isStarted() {
        assertThat(store.begin("bob:k1").state()).isEqualTo(IdempotencyStore.State.STARTED);
    }

    @Test
    void sameKeyWhileProcessing_isInProgress() {
        store.begin("bob:k1");

        assertThat(store.begin("bob:k1").state()).isEqualTo(IdempotencyStore.State.IN_PROGRESS);
    }

    @Test
    void completedKey_returnsSavedResponse() {
        SavedResponse saved = new SavedResponse(201, "application/json", "{\"id\":1}".getBytes(StandardCharsets.UTF_8));
        store.begin("bob:k1");
        store.complete("bob:k1", saved);

        IdempotencyStore.Begin begin = store.begin("bob:k1");

        assertThat(begin.state()).isEqualTo(IdempotencyStore.State.DONE);
        assertThat(begin.saved()).isSameAs(saved);
    }

    @Test
    void forgottenKey_canStartAgain() {
        store.begin("bob:k1");
        store.forget("bob:k1");

        assertThat(store.begin("bob:k1").state()).isEqualTo(IdempotencyStore.State.STARTED);
    }

    @Test
    void concurrentRequestsWithSameKey_onlyOneStarts() throws Exception {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<IdempotencyStore.State>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                start.await();
                return store.begin("bob:same").state();
            }));
        }

        start.countDown();
        int started = 0;
        for (Future<IdempotencyStore.State> result : results) {
            if (result.get() == IdempotencyStore.State.STARTED) {
                started++;
            }
        }
        executor.shutdown();

        assertThat(started).isEqualTo(1);
    }
}