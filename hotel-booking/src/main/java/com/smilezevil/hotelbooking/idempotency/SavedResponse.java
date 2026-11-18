package com.smilezevil.hotelbooking.idempotency;

public record SavedResponse(int status, String contentType, byte[] body) {
}