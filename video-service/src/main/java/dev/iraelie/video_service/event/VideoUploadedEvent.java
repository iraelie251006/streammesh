package dev.iraelie.video_service.event;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoUploadedEvent {
    // Event published to kafka for encoding-service to consume.
    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFileName;
    private long fileSizeBytes;
}
