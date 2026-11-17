package dev.iraelie.encoding_service.service;

import dev.iraelie.encoding_service.event.VideoEncodedEvent;
import dev.iraelie.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {
    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video.encoded";

    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[]{1920, 5000, 1080},
            new int[]{1280, 2800, 720},
            new int[]{854, 1200, 480},
            new int[]{640, 800, 360}
    );

    public void encodeVideo(VideoUploadedEvent event) {
        log.info("Starting encoding platform for movie: {}", event.getMovieId());

        String jobPath = basePath + "/" + event.getMovieId();

        try {
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

            String localVideoPath = jobPath + "/raw_video.mp4";

            downloadFromS3(event.getVideoKey(), localVideoPath);
            log.info("Raw video is downloaded to: {}", localVideoPath);

            for (int[] qualities: VIDEO_QUALITIES) {
                int width = qualities[0];
                int bitrate = qualities[1];
                int height = qualities[2];

                String qualityDir = jobPath + "/encoded/" + height + "p";
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localVideoPath, qualityDir, width, height, bitrate);

                log.info("Encoded {}p successfully", height);
            }

            String masterPlaylistPath = jobPath + "/encoded/master.m3u8";

            generateMasterPlaylist(masterPlaylistPath);
            log.info("Master Playlist generated");

            String encodedPrefix = "encoded/" + event.getMovieId() + "/";
            uploadEncodedFilesToS3(jobPath + "/encoded", encodedPrefix);
            log.info("All encoded files uploaded to S3");

            String masterPlaylistKey = encodedPrefix + "master.m3u8";
            String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlaylistKey;

            VideoEncodedEvent encodedEvent = VideoEncodedEvent.builder()
                    .movieId(event.getMovieId())
                    .hlsUrl(hlsUrl)
                    .masterPlaylistKey(masterPlaylistKey)
                    .success(true)
                    .errorMessage(null)
                    .build();

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), encodedEvent);
            log.info("VideoEncodedEvent published for movie: {}", event.getMovieId());

        } catch (Exception e) {
            log.info("Encoding failed for movie: {}", event.getMovieId());

            VideoEncodedEvent failerEvent = VideoEncodedEvent.builder()
                    .movieId(event.getMovieId())
                    .hlsUrl(null)
                    .masterPlaylistKey(null)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), failerEvent);
        } finally {
            cleanupTempFiles(jobPath);
        }
    }

    private void downloadFromS3(String videoKey, String localVideoPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .build();

        s3Client.getObject(getObjectRequest, Paths.get(localVideoPath));
    }

    private void encodeToHLS(String localVideoPath, String qualityDir, int width, int height, int bitrate) {
        String playlistPath = qualityDir + "/playlist.m3u8";
        String segmentPattern = localVideoPath + "/segment_%03d.ts";

        List<String> commands = Arrays.asList();
    }

    private void generateMasterPlaylist(String masterPlaylistPath) {
    }

    private void uploadEncodedFilesToS3(String s, String encodedPrefix) {
    }

    private void cleanupTempFiles(String jobPath) {
    }

}
