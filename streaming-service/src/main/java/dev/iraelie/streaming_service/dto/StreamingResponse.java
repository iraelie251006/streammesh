package dev.iraelie.streaming_service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {
    private String movieId;
    private String streamingUrl;    // presigned HLS master playlist URL
    private String quality;         // available qualities
    private long expiredInMinutes;  // URL expiry time
}
