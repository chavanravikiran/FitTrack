package com.fittrack.storage;

import com.fittrack.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Serves public catalog media (exercise thumbnails) from local storage during development.
 * With S3, URLs point to S3/CloudFront directly and this endpoint is not used.
 * Only keys under "catalog/" are served; private files (profile photos) have their own authenticated endpoints.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class MediaController {

    private static final String PREFIX = "/api/v1/media/";

    private final StorageService storage;

    @GetMapping(PREFIX + "**")
    public ResponseEntity<byte[]> media(HttpServletRequest request) {
        String key = request.getRequestURI().substring(PREFIX.length());
        if (!StorageKeys.isCatalog(key)) {
            throw new ResourceNotFoundException("Media not found");
        }
        String ext = key.substring(key.lastIndexOf('.') + 1);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(StorageService.contentTypeFor(ext)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(storage.load(key));
    }
}
