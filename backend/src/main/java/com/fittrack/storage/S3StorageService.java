package com.fittrack.storage;

import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Optional;

/**
 * AWS S3 storage. The bucket should stay private (Block Public Access on); files are served through
 * presigned URLs or a CloudFront distribution configured via app.storage.public-base-url.
 * Objects are encrypted at rest with SSE-S3 (AES256).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "S3")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String prefix;

    public S3StorageService(S3Client s3, S3Presigner presigner, AppProperties props) {
        this.s3 = s3;
        this.presigner = presigner;
        AppProperties.S3 cfg = props.getStorage().getS3();
        if (!StringUtils.hasText(cfg.getBucket())) {
            throw new IllegalStateException("app.storage.s3.bucket (AWS_S3_BUCKET) must be set when STORAGE_TYPE=S3");
        }
        this.bucket = cfg.getBucket();
        String p = cfg.getPrefix() == null ? "" : cfg.getPrefix().trim();
        this.prefix = p.isEmpty() || p.endsWith("/") ? p : p + "/";
        log.info("Using S3 storage: bucket={} region={} prefix='{}'", bucket, cfg.getRegion(), prefix);
    }

    @Override
    public String store(String folder, byte[] content, String extension, String contentType) {
        String key = StorageKeys.newKey(folder, extension);
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey(key))
                        .contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                RequestBody.fromBytes(content));
        return key;
    }

    @Override
    public byte[] load(String key) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(objectKey(key)).build()).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey(key)).build());
        } catch (S3Exception e) {
            log.warn("Could not delete S3 object {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey(key)).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    @Override
    public Optional<String> downloadUrl(String key, Duration ttl) {
        var request = presigner.presignGetObject(b -> b
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey(key)).build()));
        return Optional.of(request.url().toString());
    }

    @Override
    public Optional<UploadTarget> uploadTarget(String folder, String extension, String contentType, Duration ttl) {
        String key = StorageKeys.newKey(folder, extension);
        var request = presigner.presignPutObject(b -> b
                .signatureDuration(ttl)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey(key))
                        .contentType(contentType)
                        .build()));
        return Optional.of(new UploadTarget(key, request.url().toString(), "PUT", contentType));
    }

    private String objectKey(String key) {
        if (!StorageKeys.isValidKey(key)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return prefix + key;
    }
}
