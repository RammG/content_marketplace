package com.tianzige.marketplace.ingest.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${marketplace.ingest.storage.base-path}")
    private String basePath;

    private Path storageRoot;

    @PostConstruct
    public void init() throws IOException {
        this.storageRoot = Paths.get(basePath);
        Files.createDirectories(storageRoot);
        log.info("Initialized local file storage at: {}", storageRoot.toAbsolutePath());
    }

    @Override
    public String store(InputStream inputStream, String filename) throws IOException {
        String fileReference = UUID.randomUUID() + "_" + sanitizeFilename(filename);
        Path targetPath = storageRoot.resolve(fileReference);

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored file: {} -> {}", filename, fileReference);

        return fileReference;
    }

    @Override
    public InputStream retrieve(String fileReference) throws IOException {
        Path filePath = storageRoot.resolve(fileReference);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileReference);
        }
        return Files.newInputStream(filePath);
    }

    @Override
    public void delete(String fileReference) throws IOException {
        Path filePath = storageRoot.resolve(fileReference);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.debug("Deleted file: {}", fileReference);
        }
    }

    @Override
    public boolean exists(String fileReference) {
        return Files.exists(storageRoot.resolve(fileReference));
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
