package dev.iraelie.encoding_service.service;

import dev.iraelie.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {
    private final EncodingService encodingService;

    @KafkaListener(topics = "video.uploaded", groupId = "encoding-service-group")
    public void consumeVideoUploadedEvent(VideoUploadedEvent event) {
        log.info("Consumed video.uploaded event for movie: {}, file: {}", event.getMovieId(), event.getOriginalFileName());

        try {
            encodingService.encodeVideo(event);
        } catch (Exception e) {
            log.error("Failed to process encoding for movie: {} - {}", event.getMovieId(), e.getMessage());
        }
    }
}
