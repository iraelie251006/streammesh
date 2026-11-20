package dev.iraelie.encoding_service.service;

import dev.iraelie.encoding_service.event.VideoEncodedEvent;
import dev.iraelie.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
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

    private void encodeToHLS(
            String localVideoPath,
            String qualityDir,
            int width,
            int height,
            int bitrate
    ) throws IOException, InterruptedException {
        String playlistPath = qualityDir + "/playlist.m3u8";
        String segmentPattern = localVideoPath + "/segment_%03d.ts";

        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", localVideoPath,                       // input file
                "-vf", "scale=" + width + ":" + height,     // scale to resolution
                "-c:v", "libx264",                          // video codec, libx264(H.264), libx265(H.265)
                "-b:v", bitrate + "k",                      // video bitrate
                "-c:a", "aac",                              // Audio codec
                "-b:a", "128k",                             // Audio bitrate
                "-hls_time", "10",                          // 10 second segment
                "-hls_list_size", "0",                      // keep all segments
                "-hls_segment_filename", segmentPattern,    // segment naming
                "-f", "hls",                                // output format HLS
                playlistPath                                // output playlist
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg encoding failed with exit code: " + exitCode);
        }
    }

    private void generateMasterPlaylist(String masterPlaylistPath) throws IOException{
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("#EXT-X-VERSION:3\n\n");

        int[][] qualities = {
                {1920, 5000, 1080},
                {1280, 2800, 720},
                {854, 1200, 480},
                {640, 800, 360}
        };

        for (int[] q: qualities) {
            int width = q[0];
            int bitrate = q[1];
            int height = q[2];

            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrate * 1000)
                    .append(",RESOLUTION=")
                    .append(width)
                    .append("x")
                    .append(height)
                    .append(",CODECS=\"avc1.42e01e, mp4a.40.2\"\n");

            master.append(height).append("p/playlist.m3u8\n\n");
        }

        Files.writeString(Paths.get(masterPlaylistPath), master.toString());
    }

    private void uploadEncodedFilesToS3(String localDir, String s3EncodedPrefix) {
        File directory = new File(localDir);
        uploadDirectoryToS3(directory, localDir, s3EncodedPrefix);
    }

    private void uploadDirectoryToS3(File directory, String localDir, String s3EncodedPrefix) {
        for (File file: directory.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectoryToS3(file, localDir, s3EncodedPrefix);
            } else {
                String relativePath = file.getAbsolutePath()
                        .substring(localDir.length() + 1)
                        .replace("\\", "/");

                String s3Key = s3EncodedPrefix + relativePath;

                String contentType = file.getName().endsWith(".m3u8") ? "application/x-mpegURL" : "video/MP2T";

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                log.debug("Uploaded: {}", s3Key);
            }
        }
    }

    private void cleanupTempFiles(String jobPath) {
        try {
            Path dirPath = Paths.get(jobPath);
            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                log.info("Temp files cleaned up for job: {}", jobPath);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup Temp file: {}", e.getMessage());
        }
    }

}
