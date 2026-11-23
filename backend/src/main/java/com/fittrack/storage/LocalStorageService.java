package com.fittrack.storage;

import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/** Development storage on the local disk. Use S3 in production (app.storage.type=S3). */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AppProperties props) {
        this.root = Path.of(props.getStorage().getRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root " + root, e);
        }
        log.info("Using local file storage at {}", root);
    }

    @Override
    public String store(String folder, byte[] content, String extension, String contentType) {
        String key = StorageKeys.newKey(folder, extension);
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (NoSuchFileException e) {
            throw new ResourceNotFoundException("File not found");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public Optional<String> downloadUrl(String key, Duration ttl) {
        return Optional.empty();
    }

    @Override
    public Optional<UploadTarget> uploadTarget(String folder, String extension, String contentType, Duration ttl) {
        return Optional.empty();
    }

    /** Resolves a key inside the root and rejects path traversal. */
    private Path resolve(String key) {
        if (!StorageKeys.isValidKey(key)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Path traversal attempt");
        }
        return p;
    }
}
