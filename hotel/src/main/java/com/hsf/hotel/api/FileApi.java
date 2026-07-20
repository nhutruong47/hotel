package com.hsf.hotel.api;

import com.hsf.hotel.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/uploads")
public class FileApi {

    private final FileStorageService fileStorageService;

    public FileApi(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = fileStorageService.loadAsResource(filename);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        String safeName = file.getFilename() != null ? file.getFilename().replace("\"", "") : "file";
        String lower = safeName.toLowerCase(Locale.ROOT);
        MediaType mediaType = lower.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(file);
    }
}
