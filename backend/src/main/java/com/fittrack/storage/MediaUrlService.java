package com.fittrack.storage;

import com.fittrack.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Turns a storage key for public catalog media into a URL the browser can load:
 * CDN base URL if configured, otherwise an S3 presigned URL, otherwise the API's media endpoint (local disk).
 */
@Service
@RequiredArgsConstructor
public class MediaUrlService {

    private final StorageService storage;
    private final AppProperties props;

    public String catalogUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String base = props.getStorage().getPublicBaseUrl();
        if (StringUtils.hasText(base)) {
            String prefix = props.getStorage().getS3().getPrefix() == null ? "" : props.getStorage().getS3().getPrefix();
            return base.replaceAll("/+$", "") + "/" + prefix + key;
        }
        return storage.downloadUrl(key, props.getStorage().getPresignTtl())
                .orElse("/api/v1/media/" + key);
    }
}
