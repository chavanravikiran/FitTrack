package com.fittrack.storage;

import com.fittrack.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 clients, created only when app.storage.type=S3. Credentials use the standard AWS provider chain
 * (environment variables, shared credentials file, or the EC2/ECS/EKS IAM role) - never hard-coded.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "S3")
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(AppProperties props) {
        AppProperties.S3 s3 = props.getStorage().getS3();
        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyle()).build());
        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(AppProperties props) {
        AppProperties.S3 s3 = props.getStorage().getS3();
        var builder = S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyle()).build());
        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }
        return builder.build();
    }
}
