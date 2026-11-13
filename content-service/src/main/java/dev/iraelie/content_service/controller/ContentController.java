package dev.iraelie.content_service.controller;

import dev.iraelie.content_service.dto.MovieRequest;
import dev.iraelie.content_service.dto.MovieResponse;
import dev.iraelie.content_service.model.Genre;
import dev.iraelie.content_service.services.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Slf4j
public class ContentController {
    private final ContentService contentService;

    @PostMapping("/")
    public ResponseEntity<MovieResponse> addMovie(
            @Valid @RequestBody MovieRequest movieRequest
    ) {
        MovieResponse movieResponse = contentService.addMovie(movieRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movieResponse);
    }

    @GetMapping("/")
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        List<MovieResponse> movies = contentService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(
            @PathVariable Genre genre
            ) {
        List<MovieResponse> movies = contentService.getMoviesByGenre(genre);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMoviesById(
            @PathVariable String movieId
    ) {
        MovieResponse movie = contentService.getMoviesById(movieId);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String title
    ) {
        List<MovieResponse> movies = contentService.searchMovies(title);
        return ResponseEntity.ok(movies);
    }
}
