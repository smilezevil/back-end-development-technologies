package com.smilezevil.common.dto;

public record NotificationRequest(String recipient, String subject, String message) {
}