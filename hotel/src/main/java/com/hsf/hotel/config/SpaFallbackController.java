package com.hsf.hotel.config;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class SpaFallbackController {

    private final Path frontendDist = Paths.get("../frontend/dist").toAbsolutePath().normalize();
    private final Path indexHtml = frontendDist.resolve("index.html");

    @RequestMapping(value = "/{path:[^\\.]*}", produces = "text/html")
    public Resource index() throws IOException {
        if (Files.exists(indexHtml)) {
            return new FileSystemResource(indexHtml);
        }
        throw new IOException("Frontend dist not built. Run `npm run build` in ../frontend/");
    }
}