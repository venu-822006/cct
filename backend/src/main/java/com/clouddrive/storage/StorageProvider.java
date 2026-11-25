package com.clouddrive.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * StorageProvider abstraction interface for Personal Cloud Drive.
 * Supports Local Filesystem, MinIO (S3-compatible Object Storage), and future cloud storage adapters.
 */
public interface StorageProvider {

    /**
     * Store file content from an InputStream.
     *
     * @param storageKey  unique object/file key (e.g. "users/alice/abc12345_report.pdf")
     * @param inputStream source data stream
     * @param size        size in bytes (-1 if unknown)
     * @param contentType MIME content type
     * @return stored storage key or path identifier
     */
    String store(String storageKey, InputStream inputStream, long size, String contentType);

    /**
     * Store file content from a byte array.
     *
     * @param storageKey  unique object/file key
     * @param data        raw content bytes
     * @param contentType MIME content type
     * @return stored storage key or path identifier
     */
    String store(String storageKey, byte[] data, String contentType);

    /**
     * Load file content as Spring Resource for streaming, in-browser preview, and download.
     *
     * @param storageKey storage key or path identifier
     * @return readable Spring Resource
     */
    Resource loadAsResource(String storageKey);

    /**
     * Load file as raw InputStream.
     *
     * @param storageKey storage key or path identifier
     * @return InputStream
     */
    InputStream loadAsStream(String storageKey);

    /**
     * Delete file permanently from storage.
     *
     * @param storageKey storage key or path identifier
     * @return true if deleted or did not exist, false on error
     */
    boolean delete(String storageKey);

    /**
     * Check if a file exists in storage.
     *
     * @param storageKey storage key or path identifier
     * @return true if exists
     */
    boolean exists(String storageKey);

    /**
     * Returns provider type name, e.g. "MINIO" or "LOCAL".
     *
     * @return provider type name
     */
    String getProviderType();
}
