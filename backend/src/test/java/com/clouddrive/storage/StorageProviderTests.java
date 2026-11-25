package com.clouddrive.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageProviderTests {

    @TempDir
    Path tempDir;

    private LocalStorageProvider localStorageProvider;
    private MinioStorageProvider minioStorageProvider;
    private MinioProperties minioProperties;

    @BeforeEach
    void setUp() {
        localStorageProvider = new LocalStorageProvider(tempDir.toString());
        minioProperties = new MinioProperties();
        minioProperties.setEndpoint("http://localhost:9000");
        minioProperties.setBucket("test-bucket");
        minioProperties.setAutoCreateBucket(false);

        io.minio.MinioClient dummyClient = io.minio.MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials("dummy", "dummy")
                .build();

        minioStorageProvider = new MinioStorageProvider(dummyClient, minioProperties, localStorageProvider);
    }

    @Test
    void testLocalStorageProvider_StoreAndLoadBytes() throws Exception {
        String key = "users/tester/doc.txt";
        byte[] content = "Hello Local Storage!".getBytes(StandardCharsets.UTF_8);

        String storedKey = localStorageProvider.store(key, content, "text/plain");
        assertNotNull(storedKey);
        assertTrue(localStorageProvider.exists(key));

        Resource resource = localStorageProvider.loadAsResource(key);
        assertNotNull(resource);
        assertTrue(resource.exists());

        try (InputStream is = localStorageProvider.loadAsStream(key)) {
            String readContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello Local Storage!", readContent);
        }

        assertTrue(localStorageProvider.delete(key));
        assertFalse(localStorageProvider.exists(key));
    }

    @Test
    void testLocalStorageProvider_StoreInputStream() throws Exception {
        String key = "users/tester/stream.txt";
        byte[] content = "Streaming Data Content".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(content);

        String stored = localStorageProvider.store(key, bais, content.length, "text/plain");
        assertNotNull(stored);
        assertTrue(localStorageProvider.exists(key));

        try (InputStream is = localStorageProvider.loadAsStream(key)) {
            String read = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Streaming Data Content", read);
        }
    }

    @Test
    void testLocalStorageProvider_PathTraversalPrevention() {
        assertThrows(SecurityException.class, () -> {
            localStorageProvider.store("../../../etc/passwd", "malicious".getBytes(StandardCharsets.UTF_8), "text/plain");
        });
    }

    @Test
    void testMinioStorageProvider_KeySanitization() {
        assertEquals("users/tester/file.txt", minioStorageProvider.sanitizeObjectKey("users/tester/file.txt"));
        assertEquals("users/tester/file.txt", minioStorageProvider.sanitizeObjectKey("C:\\Users\\admin\\app\\users\\tester\\file.txt"));
        assertEquals("users/tester/file.txt", minioStorageProvider.sanitizeObjectKey("/users/tester/file.txt"));
        assertEquals("sample.pdf", minioStorageProvider.sanitizeObjectKey("/sample.pdf"));
    }

    @Test
    void testMinioStorageProvider_GracefulFallbackWhenMinioOffline() throws Exception {
        // Since MinIO is not running on localhost:9000, provider must cleanly fallback to local storage without throwing
        String key = "users/tester/minio_fallback.txt";
        byte[] data = "MinIO Fallback Content".getBytes(StandardCharsets.UTF_8);

        String storedKey = minioStorageProvider.store(key, data, "text/plain");
        assertNotNull(storedKey);
        assertTrue(minioStorageProvider.exists(key));

        Resource resource = minioStorageProvider.loadAsResource(key);
        assertNotNull(resource);
        assertTrue(resource.exists());

        try (InputStream is = minioStorageProvider.loadAsStream(key)) {
            String read = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("MinIO Fallback Content", read);
        }

        assertTrue(minioStorageProvider.delete(key));
        assertFalse(minioStorageProvider.exists(key));
    }
}
