package com.example.profileservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.MakeBucketArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.secure}")
    private boolean secure;

    @Value("${app.minio.region}")
    private String region;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();

        // Initialize bucket on startup
        try {
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).region(region).build());
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create MinIO bucket '{}': {}", bucket, e.getMessage());
        }

        return client;
    }
}