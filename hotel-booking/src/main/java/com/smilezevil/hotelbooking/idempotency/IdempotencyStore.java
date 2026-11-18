package com.smilezevil.hotelbooking.idempotency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyStore {

    public enum State { STARTED, IN_PROGRESS, DONE }

    public record Begin(State state, SavedResponse saved) {
    }

    private static final SavedResponse IN_PROGRESS = new SavedResponse(0, null, new byte[0]);

    private final Map<String, SavedResponse> entries = new ConcurrentHashMap<>();

    public Begin begin(String key) {
        SavedResponse previous = entries.putIfAbsent(key, IN_PROGRESS);
        if (previous == null) {
            return new Begin(State.STARTED, null);
        }
        if (previous == IN_PROGRESS) {
            return new Begin(State.IN_PROGRESS, null);
        }
        return new Begin(State.DONE, previous);
    }

    public void complete(String key, SavedResponse response) {
        entries.put(key, response);
    }

    public void forget(String key) {
        entries.remove(key);
    }
}