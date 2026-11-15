package dev.iraelie.content_service.services;

import dev.iraelie.content_service.dto.MovieRequest;
import dev.iraelie.content_service.dto.MovieResponse;
import dev.iraelie.content_service.model.Genre;
import dev.iraelie.content_service.model.Movie;
import dev.iraelie.content_service.model.VideoStatus;
import dev.iraelie.content_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {
    private final MovieRepository movieRepository;

    public MovieResponse addMovie(MovieRequest movieRequest) {
        log.info("Adding new movie: {}", movieRequest.getTitle());

        Movie movie = Movie.builder()
                .title(movieRequest.getTitle())
                .description(movieRequest.getDescription())
                .genre(movieRequest.getGenre())
                .director(movieRequest.getDirector())
                .cast(movieRequest.getCast())
                .rating(movieRequest.getRating())
                .durationMinutes(movieRequest.getDurationMinutes())
                .releaseYear(movieRequest.getReleaseYear())
                .thumbnail_url(movieRequest.getThumbnail_url())
                .videoStatus(VideoStatus.PENDING)
                .build();

        Movie savedMovie = movieRepository.save(movie);
        log.info("Movie added with id: {}", savedMovie.getId());

        return mapToResponse(savedMovie);
    }

    private MovieResponse mapToResponse(Movie savedMovie) {

        return MovieResponse.builder()
                .id(savedMovie.getId())
                .title(savedMovie.getTitle())
                .description(savedMovie.getDescription())
                .genre(savedMovie.getGenre())
                .director(savedMovie.getDirector())
                .cast(savedMovie.getCast())
                .rating(savedMovie.getRating())
                .durationMinutes(savedMovie.getDurationMinutes())
                .releaseYear(savedMovie.getReleaseYear())
                .thumbnail_url(savedMovie.getThumbnail_url())
                .hlsUrl(savedMovie.getHlsUrl())
                .videoKey(savedMovie.getVideoKey())
                .videoStatus(savedMovie.getVideoStatus())
                .createdAt(savedMovie.getCreatedAt())
                .build();
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenre(genre)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MovieResponse getMoviesById(String movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        return mapToResponse(movie);
    }

    public List<MovieResponse> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void updateVideoKey(String movieId, String videoKey) {
        log.info("Updating videoKey for movie: {}", movieId);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

    public void updateHlsUrl(String movieId, String hlsUrl) {
        log.info("Updating HLS URL for movie: {}", movieId);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);

        log.info("Movie {} is now ready for streaming", movieId);
    }
}
