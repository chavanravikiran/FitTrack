package com.fittrack.storage;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction over binary file storage. {@link LocalStorageService} (development) and
 * {@link S3StorageService} (AWS S3) implement it, so business code never depends on where files live.
 */
public interface StorageService {

    /** Stores the bytes and returns an opaque relative key, e.g. {@code profile-photos/12/uuid.jpg}. */
    default String store(String folder, byte[] content, String extension) {
        return store(folder, content, extension, contentTypeFor(extension));
    }

    String store(String folder, byte[] content, String extension, String contentType);

    byte[] load(String key);

    void delete(String key);

    boolean exists(String key);

    /** A time-limited direct download URL (S3 presigned GET). Empty for storage that can't issue one. */
    Optional<String> downloadUrl(String key, Duration ttl);

    /**
     * A time-limited direct upload target (S3 presigned PUT) so large files (videos) go straight from the
     * browser to storage instead of through the API. Empty when unsupported.
     */
    Optional<UploadTarget> uploadTarget(String folder, String extension, String contentType, Duration ttl);

    record UploadTarget(String key, String url, String method, String contentType) {
    }

    static String contentTypeFor(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }
}
