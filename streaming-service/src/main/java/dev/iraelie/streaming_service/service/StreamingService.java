package dev.iraelie.streaming_service.service;

import dev.iraelie.streaming_service.dto.StreamingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {
    private final S3Presigner s3Presigner;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${aws.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry;

    private static final String STREAMING_URL_CACHE_PREFIX = "streaming:url:";

    public StreamingResponse getStreamingUrl(String movieId, String playlistKey) {
        log.info("Getting streaming URL for movie: {}", movieId);

        String cache_key = STREAMING_URL_CACHE_PREFIX + movieId;

        String cachedUrl = redisTemplate.opsForValue()
                .get(cache_key);

        if (cachedUrl != null) {
            log.info("Returning streaming URL for movie: {}", movieId);
            return StreamingResponse.builder()
                    .movieId(movieId)
                    .streamingUrl(cachedUrl)
                    .quality("1080p, 720p, 460p, 360p")
                    .expiredInMinutes(presignedUrlExpiry)
                    .build();
        }

        log.info("Generating new presigned url for movie: {}", movieId);

        String presignedUrl = generatePresignedUrl(playlistKey);

        redisTemplate.opsForValue().set(cache_key, presignedUrl, Duration.ofMinutes(55));

        log.info("Streaming URL generated and cached for movie: {}", movieId);

        return StreamingResponse.builder()
                .movieId(movieId)
                .streamingUrl(presignedUrl)
                .quality("1080p, 720p, 460p, 360p")
                .expiredInMinutes(presignedUrlExpiry)
                .build();
    }

    private String generatePresignedUrl(String playlistKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(playlistKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiry))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void invalidateCache(String movieId) {
        String cacheKey = STREAMING_URL_CACHE_PREFIX + movieId;
        redisTemplate.delete(cacheKey);

        log.info("Streaming URL cache invalidated for movie: {}", movieId);
    }
}
