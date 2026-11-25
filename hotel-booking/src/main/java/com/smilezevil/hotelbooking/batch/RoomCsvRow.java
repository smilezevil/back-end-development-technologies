package com.smilezevil.hotelbooking.batch;

import org.springframework.batch.item.file.transform.FieldSet;

public record RoomCsvRow(int lineNumber,
                         String hotelId,
                         String roomNumber,
                         String type,
                         String pricePerNight,
                         String capacity) {

    public static final String[] COLUMNS = {"hotelId", "roomNumber", "type", "pricePerNight", "capacity"};

    public static RoomCsvRow from(int lineNumber, FieldSet fields) {
        return new RoomCsvRow(lineNumber,
                fields.readString("hotelId"),
                fields.readString("roomNumber"),
                fields.readString("type"),
                fields.readString("pricePerNight"),
                fields.readString("capacity"));
    }
}