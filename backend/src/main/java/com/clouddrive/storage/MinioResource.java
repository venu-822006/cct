package com.clouddrive.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.core.io.AbstractResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Spring Resource adapter for objects stored in MinIO S3 object storage.
 * Supports streaming, range requests, and content metadata.
 */
public class MinioResource extends AbstractResource {

    private final MinioClient minioClient;
    private final String bucket;
    private final String objectKey;
    private final String filename;
    private Long cachedSize;

    public MinioResource(MinioClient minioClient, String bucket, String objectKey, String filename) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.filename = filename != null ? filename : extractFilename(objectKey);
    }

    private static String extractFilename(String key) {
        if (key == null) return "unknown";
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }

    @Override
    public String getDescription() {
        return "MinIO object [" + bucket + "/" + objectKey + "]";
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public boolean exists() {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
            return stat != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long contentLength() throws IOException {
        if (cachedSize != null) {
            return cachedSize;
        }
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
            cachedSize = stat.size();
            return cachedSize;
        } catch (Exception e) {
            throw new IOException("Failed to obtain content length for MinIO object: " + objectKey, e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
        } catch (Exception e) {
            throw new FileNotFoundException("Could not open MinIO object: " + bucket + "/" + objectKey + " - " + e.getMessage());
        }
    }
}
