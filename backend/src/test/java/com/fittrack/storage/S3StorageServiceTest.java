package com.fittrack.storage;

import com.fittrack.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3StorageServiceTest {

    private S3Client s3;
    private S3StorageService service;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        // Presigning is pure local signing, so real credentials are not needed for the test.
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_SOUTH_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATEST", "secret")))
                .build();
        AppProperties props = new AppProperties();
        props.getStorage().getS3().setBucket("fittrack-media");
        props.getStorage().getS3().setPrefix("prod");
        service = new S3StorageService(s3, presigner, props);
    }

    @Test
    void storesWithPrefixContentTypeAndEncryption() {
        String key = service.store("catalog/thumbnails", new byte[]{1, 2, 3}, "jpg", "image/jpeg");

        ArgumentCaptor<PutObjectRequest> req = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(req.capture(), any(RequestBody.class));
        assertThat(key).startsWith("catalog/thumbnails/").endsWith(".jpg");
        assertThat(req.getValue().bucket()).isEqualTo("fittrack-media");
        assertThat(req.getValue().key()).isEqualTo("prod/" + key);
        assertThat(req.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(req.getValue().serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
    }

    @Test
    void issuesPresignedUploadAndDownloadUrls() {
        var upload = service.uploadTarget("catalog/videos", "mp4", "video/mp4", Duration.ofMinutes(15)).orElseThrow();
        assertThat(upload.method()).isEqualTo("PUT");
        assertThat(upload.url()).contains("fittrack-media").contains("prod/catalog/videos/").contains("X-Amz-Signature");

        String download = service.downloadUrl(upload.key(), Duration.ofHours(1)).orElseThrow();
        assertThat(download).contains("X-Amz-Expires=3600");
    }

    @Test
    void rejectsUnsafeKeys() {
        assertThatThrownBy(() -> service.load("../secrets.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.store("../etc", new byte[0], "jpg")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresBucket() {
        assertThatThrownBy(() -> new S3StorageService(s3, mock(S3Presigner.class), new AppProperties()))
                .isInstanceOf(IllegalStateException.class);
    }
}
