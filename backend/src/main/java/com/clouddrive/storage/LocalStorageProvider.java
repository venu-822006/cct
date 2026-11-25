package com.clouddrive.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;

/**
 * Local filesystem implementation of StorageProvider.
 */
public class LocalStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageProvider.class);

    private final Path rootPath;

    public LocalStorageProvider(String baseDir) {
        this.rootPath = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
            log.info("Initialized LocalStorageProvider root directory at: {}", this.rootPath);
        } catch (IOException e) {
            log.error("Could not create local storage root directory: {}", this.rootPath, e);
        }
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Path resolvePath(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new IllegalArgumentException("Storage key cannot be empty");
        }

        // Check if the key is already an absolute path or existing filesystem path
        Path directPath = Paths.get(storageKey);
        if (directPath.isAbsolute() && Files.exists(directPath)) {
            return directPath.normalize();
        }

        // Clean relative key
        String cleanKey = storageKey.replace('\\', '/').replaceAll("^/+", "");
        if (cleanKey.contains("..")) {
            throw new SecurityException("Storage key contains forbidden traversal sequence '..': " + cleanKey);
        }

        Path resolved = rootPath.resolve(cleanKey).normalize();
        if (!resolved.startsWith(rootPath) && !Files.exists(directPath)) {
            throw new SecurityException("Storage key escapes storage root: " + cleanKey);
        }
        return resolved;
    }

    @Override
    public String store(String storageKey, InputStream inputStream, long size, String contentType) {
        Path target = resolvePath(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on local disk: " + target, e);
        }
    }

    @Override
    public String store(String storageKey, byte[] data, String contentType) {
        Path target = resolvePath(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file bytes on local disk: " + target, e);
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        Path path = resolvePath(storageKey);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileNotFoundException("Local file not found or not readable: " + path);
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new RuntimeException("Failed to load local resource for key: " + storageKey, e);
        }
    }

    @Override
    public InputStream loadAsStream(String storageKey) {
        Path path = resolvePath(storageKey);
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open local input stream for key: " + storageKey, e);
        }
    }

    @Override
    public boolean delete(String storageKey) {
        try {
            Path path = resolvePath(storageKey);
            return Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete local file for key: {}", storageKey, e);
            return false;
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path path = resolvePath(storageKey);
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderType() {
        return "LOCAL";
    }
}
