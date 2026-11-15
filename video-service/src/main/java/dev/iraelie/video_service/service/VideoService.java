package dev.iraelie.video_service.service;

import dev.iraelie.video_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {
    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private final String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video.uploaded";

    public String uploadVideo(String movieId, MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        log.info("Starting video upload for movie: {}, filename: {}", movieId, fileName);

        // key format = raw/movieId/uuid_filename
        String videoKey = "raw/" + movieId + "/" + UUID.randomUUID() + "_" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Video uploaded to S3");

        VideoUploadedEvent event = VideoUploadedEvent.builder()
                .movieId(movieId)
                .bucketName(bucketName)
                .originalFileName(fileName)
                .videoKey(videoKey)
                .fileSizeBytes(file.getSize())
                .build();

        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC, movieId, event);
        log.info("VideoUploadedEvent published for movie: {}", movieId);

        return videoKey;
    }
}
