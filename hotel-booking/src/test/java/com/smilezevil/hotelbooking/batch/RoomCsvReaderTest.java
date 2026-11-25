package com.smilezevil.hotelbooking.batch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomCsvReaderTest {

    @TempDir
    Path importDir;

    private FlatFileItemReader<RoomCsvRow> reader;

    @AfterEach
    void closeReader() {
        if (reader != null) {
            reader.close();
        }
    }

    @Test
    void read_skipsHeaderAndKeepsLineNumbersFromFile() throws Exception {
        write("rooms.csv", """
                hotelId,roomNumber,type,pricePerNight,capacity
                1,301,STANDARD,1200.00,2
                1, 302 ,suite,3500,4
                """);
        reader = open("rooms.csv");

        assertThat(reader.read()).isEqualTo(new RoomCsvRow(2, "1", "301", "STANDARD", "1200.00", "2"));
        assertThat(reader.read()).isEqualTo(new RoomCsvRow(3, "1", "302", "suite", "3500", "4"));
        assertThat(reader.read()).isNull();
    }

    @Test
    void read_wrongColumnCount_throwsParseExceptionWithLineAndInput() throws Exception {
        write("rooms.csv", """
                hotelId,roomNumber,type,pricePerNight,capacity
                1,301,STANDARD,1200.00,2
                1,302,SUITE
                """);
        reader = open("rooms.csv");
        reader.read();

        assertThatThrownBy(reader::read)
                .isInstanceOfSatisfying(FlatFileParseException.class, e -> {
                    assertThat(e.getLineNumber()).isEqualTo(3);
                    assertThat(e.getInput()).isEqualTo("1,302,SUITE");
                });
    }

    private FlatFileItemReader<RoomCsvRow> open(String fileName) {
        ImportFileResolver resolver = new ImportFileResolver(new RoomImportProperties(importDir.toString(), 5, 10));
        FlatFileItemReader<RoomCsvRow> csvReader = new RoomImportJobConfig().roomCsvReader(fileName, resolver);
        csvReader.open(new ExecutionContext());
        return csvReader;
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(importDir.resolve(name), content);
    }
}