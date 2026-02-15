package com.faturaocr.infrastructure.adapter.storage;

import com.faturaocr.domain.invoice.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class FileSystemStorageAdapter implements FileStoragePort {

    private final String storagePath;

    public FileSystemStorageAdapter(@Value("${upload.storage-path:/data/invoices}") String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public String saveFile(InputStream inputStream, String companyId, String fileName) {
        try {
            LocalDate now = LocalDate.now();
            String year = String.valueOf(now.getYear());
            String month = String.format("%02d", now.getMonthValue());

            Path directory = Paths.get(storagePath, companyId, year, month);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            Path filePath = directory.resolve(uniqueFileName);

            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + fileName, e);
        }
    }

    @Override
    public InputStream readFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new RuntimeException("File not found: " + filePath);
            }
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file " + filePath, e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log warning but don't throw exception to avoid breaking transaction if called
            // in cleanup
            System.err.println("Failed to delete file " + filePath + ": " + e.getMessage());
        }
    }
}
