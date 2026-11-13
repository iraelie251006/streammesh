package dev.iraelie.content_service.dto;

import dev.iraelie.content_service.model.Genre;
import dev.iraelie.content_service.model.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
    private String id;
    private String title;
    private String description;
    private Genre genre;
    private String director;
    private String cast;
    private int releaseYear;
    private double rating;
    private String thumbnail_url;
    private int durationMinutes;
    private String videoKey;
    private String hlsUrl;
    private VideoStatus videoStatus;
    private LocalDateTime createdAt;
}
