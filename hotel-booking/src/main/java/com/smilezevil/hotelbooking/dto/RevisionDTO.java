package com.smilezevil.hotelbooking.dto;

import java.time.LocalDateTime;

public record RevisionDTO(Integer revision, LocalDateTime changedAt, String changedBy, String change, Object snapshot) {
}