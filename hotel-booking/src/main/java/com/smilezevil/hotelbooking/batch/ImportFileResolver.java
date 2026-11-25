package com.smilezevil.hotelbooking.batch;

import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class ImportFileResolver {

    private final Path directory;

    public ImportFileResolver(RoomImportProperties properties) {
        this.directory = Path.of(properties.directory()).toAbsolutePath().normalize();
    }

    public Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Вкажіть ім'я файлу для імпорту");
        }
        Path file = directory.resolve(fileName).normalize();
        if (!directory.equals(file.getParent())) {
            throw new IllegalArgumentException(
                    "Недопустиме ім'я файлу '" + fileName + "': можна вказати лише файл з папки імпорту");
        }
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Імпортувати можна лише файли .csv");
        }
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Файл '" + fileName + "' не знайдено в папці імпорту");
        }
        if (!realPath(file).startsWith(realPath(directory))) {
            throw new IllegalArgumentException("Файл '" + fileName + "' веде за межі папки імпорту");
        }
        return file;
    }

    private static Path realPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}