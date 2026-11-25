package com.clouddrive.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Storage provider Spring configuration.
 * Selects MinioStorageProvider or LocalStorageProvider based on configuration.
 */
@Configuration
public class StorageConfig {

    @Value("${app.storage.base-dir:./storage_root}")
    private String baseDir;

    @Value("${app.storage.type:minio}")
    private String storageType;

    @Bean
    public LocalStorageProvider localStorageProvider() {
        return new LocalStorageProvider(baseDir);
    }

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .region(properties.getRegion())
                .build();
    }

    @Bean
    @Primary
    public StorageProvider storageProvider(LocalStorageProvider localProvider,
                                           MinioProperties minioProperties,
                                           MinioClient minioClient) {
        if ("minio".equalsIgnoreCase(storageType) || "s3".equalsIgnoreCase(storageType)) {
            return new MinioStorageProvider(minioClient, minioProperties, localProvider);
        }
        return localProvider;
    }
}
