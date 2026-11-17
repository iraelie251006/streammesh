package dev.iraelie.encoding_service.event;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoEncodedEvent {
    private String movieId;
    private String hlsUrl;
    private String masterPlaylistKey;
    private boolean success;
    private String errorMessage;
}
