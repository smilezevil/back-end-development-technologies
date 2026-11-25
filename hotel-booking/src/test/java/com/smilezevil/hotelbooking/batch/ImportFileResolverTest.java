package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportFileResolverTest {

    @TempDir
    Path tempDir;

    private Path importDir;
    private ImportFileResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        importDir = Files.createDirectory(tempDir.resolve("imports"));
        resolver = new ImportFileResolver(new RoomImportProperties(importDir.toString(), 5, 10));
    }

    @Test
    void resolve_csvInImportDirectory_returnsIt() throws IOException {
        Path file = Files.writeString(importDir.resolve("rooms.csv"), "hotelId\n");

        assertThat(resolver.resolve("rooms.csv")).isEqualTo(file);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside.csv", "../../etc/passwd", "/etc/hosts", "nested/rooms.csv", "..", "."})
    void resolve_pathOutsideImportDirectory_isRejected(String fileName) {
        assertThatThrownBy(() -> resolver.resolve(fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("можна вказати лише файл з папки імпорту");
    }

    @Test
    void resolve_symlinkLeadingOutside_isRejected() throws IOException {
        Path secret = Files.writeString(tempDir.resolve("secret.csv"), "hotelId\n");
        Files.createSymbolicLink(importDir.resolve("link.csv"), secret);

        assertThatThrownBy(() -> resolver.resolve("link.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("веде за межі папки імпорту");
    }

    @Test
    void resolve_notCsv_isRejected() {
        assertThatThrownBy(() -> resolver.resolve("notes.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Імпортувати можна лише файли .csv");
    }

    @Test
    void resolve_missingFile_throwsNotFound() {
        assertThatThrownBy(() -> resolver.resolve("no-such-file.csv"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Файл 'no-such-file.csv' не знайдено в папці імпорту");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void resolve_blankName_isRejected(String fileName) {
        assertThatThrownBy(() -> resolver.resolve(fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Вкажіть ім'я файлу для імпорту");
    }
}