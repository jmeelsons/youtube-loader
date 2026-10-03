package com.example.ytdl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api")
public class DownloadController {

    @Autowired
    private DownloadService service;

    @PostMapping("/download")
    public ResponseEntity<?> download(@RequestParam String url,
                                      @RequestParam(defaultValue = "video") String type) {
        try {
            File file = service.download(url, type);
            FileSystemResource resource = new FileSystemResource(file);

            String mime = type.equalsIgnoreCase("audio")
                    ? "audio/mpeg"
                    : "video/mp4";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ошибка: " + e.getMessage());
        }
    }
}