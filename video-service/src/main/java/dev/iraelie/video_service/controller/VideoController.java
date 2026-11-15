package dev.iraelie.video_service.controller;

import dev.iraelie.video_service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/videos")
public class VideoController {
    private final VideoService videoService;

    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile file
            ) throws IOException {
        long fileSize = file.getSize() / (10124 * 1024);

        log.info("Video upload request for movie: {}, file size: {}MB", movieId, fileSize);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String videoKey = videoService.uploadVideo(movieId, file);

        return ResponseEntity.ok("Video uploaded successfully! Key: " + videoKey + " - Encoding started via Kafka");
    }
}
