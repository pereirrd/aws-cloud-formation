package com.books.repository.s3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import com.books.core.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Slf4j
@RequiredArgsConstructor
public class S3Bucket {

    private final S3Client s3Client;
    private final Configuration configuration;

    public Optional<String> readFileAsString(String fileName) {
        var s3Location = resolveS3Location(fileName);
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Location.bucket())
                .key(s3Location.key())
                .build();

        try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest);
                var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            var content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            return Optional.of(content);
        } catch (Exception exception) {
            log.error("Failed to read file {} from {}", fileName, configuration.getS3Url(), exception);
            return Optional.empty();
        }
    }

    public Optional<byte[]> readFileAsBytes(String fileName) {
        var s3Location = resolveS3Location(fileName);
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Location.bucket())
                .key(s3Location.key())
                .build();

        try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest)) {
            return Optional.of(inputStream.readAllBytes());
        } catch (Exception exception) {
            log.error("Failed to read file {} from {}", fileName, configuration.getS3Url(), exception);
            return Optional.empty();
        }
    }

    private S3Location resolveS3Location(String fileName) {
        var uri = java.net.URI.create(configuration.getS3Url());
        var bucket = uri.getHost();
        var prefix = uri.getPath();
        var normalizedPrefix = prefix == null ? "" : prefix.replaceFirst("^/", "").replaceAll("/$", "");
        var key = java.util.stream.Stream.of(normalizedPrefix, fileName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining("/"));

        return new S3Location(bucket, key);
    }

    private record S3Location(String bucket, String key) {
    }
}
