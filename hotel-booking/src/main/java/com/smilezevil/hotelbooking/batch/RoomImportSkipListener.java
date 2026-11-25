package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.entity.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;

@Slf4j
public class RoomImportSkipListener implements SkipListener<RoomCsvRow, Room> {

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException e) {
            String reason = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.warn("ROOM IMPORT skip, line {}: рядок не вдалося розібрати ({}), вміст: [{}]",
                    e.getLineNumber(), reason, e.getInput());
            return;
        }
        log.warn("ROOM IMPORT skip while reading: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(RoomCsvRow row, Throwable t) {
        String reason = t instanceof InvalidRoomRowException e ? e.getReason() : t.getMessage();
        log.warn("ROOM IMPORT skip, line {}: {}", row.lineNumber(), reason);
    }
}