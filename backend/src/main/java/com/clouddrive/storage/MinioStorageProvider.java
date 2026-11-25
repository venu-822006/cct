package com.clouddrive.storage;

import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * MinIO S3-compatible Object Storage implementation of StorageProvider.
 * Provides high-performance object persistence with automatic local filesystem fallback.
 */
public class MinioStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageProvider.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final LocalStorageProvider localStorageProvider;
    private volatile boolean minioAvailable = false;
    private volatile boolean initialized = false;

    public MinioStorageProvider(MinioClient minioClient,
                                MinioProperties properties,
                                LocalStorageProvider localStorageProvider) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.localStorageProvider = localStorageProvider;
        initializeBucket();
    }

    private synchronized void initializeBucket() {
        if (initialized) return;
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build()
            );
            if (!exists && properties.isAutoCreateBucket()) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.getBucket())
                                .region(properties.getRegion())
                                .build()
                );
                log.info("Created MinIO bucket: {}", properties.getBucket());
            }
            this.minioAvailable = true;
            this.initialized = true;
            log.info("Successfully connected to MinIO at {} (bucket: {})", properties.getEndpoint(), properties.getBucket());
        } catch (Exception e) {
            this.minioAvailable = false;
            this.initialized = true;
            log.warn("MinIO service not reachable at {} ({}). Using local disk fallback until MinIO is online.",
                    properties.getEndpoint(), e.getMessage());
        }
    }

    private boolean isMinioReady() {
        if (!initialized) {
            initializeBucket();
        }
        return minioAvailable;
    }

    public String sanitizeObjectKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Storage key cannot be empty");
        }
        String normalized = key.replace('\\', '/');
        int userIdx = normalized.indexOf("users/");
        if (userIdx >= 0) {
            return normalized.substring(userIdx);
        }
        return normalized.replaceAll("^/+", "");
    }

    @Override
    public String store(String storageKey, InputStream inputStream, long size, String contentType) {
        String cleanKey = sanitizeObjectKey(storageKey);
        if (isMinioReady()) {
            try {
                long partSize = (size <= 0 || size > 5242880) ? 10485760L : -1;
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(cleanKey)
                                .stream(inputStream, size, partSize)
                                .contentType(contentType != null ? contentType : "application/octet-stream")
                                .build()
                );
                log.debug("Stored object in MinIO: {}/{}", properties.getBucket(), cleanKey);
                return cleanKey;
            } catch (Exception e) {
                log.warn("MinIO putObject failed for key {}, delegating to local disk storage: {}", cleanKey, e.getMessage());
            }
        }
        return localStorageProvider.store(cleanKey, inputStream, size, contentType);
    }

    @Override
    public String store(String storageKey, byte[] data, String contentType) {
        String cleanKey = sanitizeObjectKey(storageKey);
        if (isMinioReady()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(cleanKey)
                                .stream(bais, data.length, -1)
                                .contentType(contentType != null ? contentType : "application/octet-stream")
                                .build()
                );
                log.debug("Stored object bytes in MinIO: {}/{}", properties.getBucket(), cleanKey);
                return cleanKey;
            } catch (Exception e) {
                log.warn("MinIO putObject (bytes) failed for key {}, delegating to local disk: {}", cleanKey, e.getMessage());
            }
        }
        return localStorageProvider.store(cleanKey, data, contentType);
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        String cleanKey = sanitizeObjectKey(storageKey);
        if (isMinioReady()) {
            try {
                MinioResource resource = new MinioResource(minioClient, properties.getBucket(), cleanKey, null);
                if (resource.exists()) {
                    return resource;
                }
            } catch (Exception e) {
                log.debug("MinIO loadAsResource failed for key {}: {}", cleanKey, e.getMessage());
            }
        }
        return localStorageProvider.loadAsResource(storageKey);
    }

    @Override
    public InputStream loadAsStream(String storageKey) {
        String cleanKey = sanitizeObjectKey(storageKey);
        if (isMinioReady()) {
            try {
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(cleanKey)
                                .build()
                );
            } catch (Exception e) {
                log.debug("MinIO loadAsStream failed for key {}: {}", cleanKey, e.getMessage());
            }
        }
        return localStorageProvider.loadAsStream(storageKey);
    }

    @Override
    public boolean delete(String storageKey) {
        String cleanKey = sanitizeObjectKey(storageKey);
        boolean deleted = false;
        if (isMinioReady()) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(cleanKey)
                                .build()
                );
                deleted = true;
                log.debug("Deleted object from MinIO: {}/{}", properties.getBucket(), cleanKey);
            } catch (Exception e) {
                log.warn("MinIO removeObject failed for key {}: {}", cleanKey, e.getMessage());
            }
        }
        if (localStorageProvider.exists(storageKey)) {
            deleted = localStorageProvider.delete(storageKey) || deleted;
        }
        return deleted;
    }

    @Override
    public boolean exists(String storageKey) {
        String cleanKey = sanitizeObjectKey(storageKey);
        if (isMinioReady()) {
            try {
                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(cleanKey)
                                .build()
                );
                if (stat != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return localStorageProvider.exists(storageKey);
    }

    @Override
    public String getProviderType() {
        return "MINIO";
    }

    public boolean isMinioAvailable() {
        return minioAvailable;
    }
}
