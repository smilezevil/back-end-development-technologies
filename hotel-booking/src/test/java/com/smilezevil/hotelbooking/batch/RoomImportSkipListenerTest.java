package com.smilezevil.hotelbooking.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RoomImportSkipListenerTest {

    private final RoomImportSkipListener listener = new RoomImportSkipListener();

    @Test
    void onSkipInProcess_logsLineNumberAndReason(CapturedOutput output) {
        RoomCsvRow row = new RoomCsvRow(7, "1", "301", "STANDARD", "-5", "2");

        listener.onSkipInProcess(row, new InvalidRoomRowException(7, "ціна має бути більшою за 0, а не -5"));

        assertThat(output).contains("ROOM IMPORT skip, line 7: ціна має бути більшою за 0, а не -5");
    }

    @Test
    void onSkipInRead_logsLineNumberReasonAndBrokenInput(CapturedOutput output) {
        FlatFileParseException error = new FlatFileParseException("Parsing error at line: 4",
                new IllegalStateException("Incorrect number of tokens found in record: expected 5 actual 3"),
                "1,302,SUITE", 4);

        listener.onSkipInRead(error);

        assertThat(output)
                .contains("ROOM IMPORT skip, line 4:")
                .contains("expected 5 actual 3")
                .contains("[1,302,SUITE]");
    }
}