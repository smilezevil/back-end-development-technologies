package com.smilezevil.hotelbooking.batch;

import lombok.Getter;

@Getter
public class InvalidRoomRowException extends RuntimeException {

    private final int lineNumber;
    private final String reason;

    public InvalidRoomRowException(int lineNumber, String reason) {
        super("Рядок " + lineNumber + ": " + reason);
        this.lineNumber = lineNumber;
        this.reason = reason;
    }
}