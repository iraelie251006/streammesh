package dev.iraelie.content_service.dto;

import dev.iraelie.content_service.model.Genre;

public class MovieRequest {
    private String title;

    private String description;

    private Genre genre;

    private String director;
    private String cast;
    private int releaseYear;
    private double rating;
    private String thumbnail_url;
    private int durationMinutes;
}
