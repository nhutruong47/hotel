package com.hsf.hotel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import com.hsf.hotel.exception.FileStorageException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif",
            "image/webp", "image/avif"
    );

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB

    private final Path storageLocation;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create upload directory", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File exceeds 5 MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new FileStorageException("Unsupported file type: " + contentType);
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int i = original.lastIndexOf('.');
        if (i >= 0) ext = original.substring(i).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new FileStorageException("Unsupported file extension: " + ext);
        }

        // Decode the image once and re-encode it. This both (a) verifies the
        // bytes are a real image and (b) strips any metadata side-channels
        // (EXIF GPS, ICC profile, etc.). Failures here surface a clean error
        // to the client instead of letting bogus files through to disk.
        byte[] sanitized;
        try (InputStream in = file.getInputStream()) {
            sanitized = ImageProcessor.sanitize(in, ext);
        } catch (IOException ex) {
            throw new FileStorageException("Invalid image: " + ex.getMessage(), ex);
        }

        String filename = UUID.randomUUID().toString() + "." + pickStorageExtension(ext);
        try {
            Path target = this.storageLocation.resolve(filename).normalize();
            if (!target.startsWith(this.storageLocation)) {
                throw new FileStorageException("Invalid file path");
            }
            Files.write(target, sanitized);
            return filename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    /**
     * Normalise the on-disk extension to {@code jpg} or {@code png} so the
     * bytes served by the static handler are decodable by every browser.
     * Browser-facing URLs use {@code .jpg} / {@code .png} regardless of what
     * the client uploaded (e.g. {@code .webp} → {@code .jpg} on disk).
     */
    private static String pickStorageExtension(String originalExt) {
        if (originalExt == null) return "jpg";
        return switch (originalExt.toLowerCase(Locale.ROOT)) {
            case ".png", ".gif", ".webp", ".avif" -> "png";
            default -> "jpg";
        };
    }

    public Resource loadAsResource(String filename) {
        if (filename == null || filename.isBlank()) return null;
        try {
            Path file = storageLocation.resolve(filename).normalize();
            if (!file.startsWith(this.storageLocation)) {
                return null;
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path file = storageLocation.resolve(filename).normalize();
            if (!file.startsWith(this.storageLocation)) return;
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}
