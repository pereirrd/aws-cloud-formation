package com.books.repository.s3;

import com.books.core.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3BucketTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private Configuration configuration;

    @Mock
    private ResponseInputStream<GetObjectResponse> responseInputStream;

    private S3Bucket s3Bucket;

    @BeforeEach
    void setUp() {
        when(configuration.getS3Url()).thenReturn("https://example-bucket.s3.amazonaws.com/path/to/files");
        s3Bucket = new S3Bucket(s3Client, configuration);
    }

    @Test
    void deveLerArquivoComoStringComSucesso() throws IOException {
        var fileName = "books.csv";
        var fileContent = "titulo,autor,genero,periodo\nDom Casmurro,Machado de Assis,Romance,Século XIX";
        var fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
        var inputStream = new ByteArrayInputStream(fileBytes);
        var realResponseInputStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                inputStream
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(realResponseInputStream);

        var resultado = s3Bucket.readFileAsString(fileName);

        assertTrue(resultado.isPresent());
        assertEquals(fileContent, resultado.get());
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void deveLerArquivoComoBytesComSucesso() throws IOException {
        var fileName = "books.csv";
        var fileContent = "titulo,autor,genero,periodo\nDom Casmurro,Machado de Assis,Romance,Século XIX";
        var fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        when(responseInputStream.readAllBytes()).thenReturn(fileBytes);

        var resultado = s3Bucket.readFileAsBytes(fileName);

        assertTrue(resultado.isPresent());
        assertEquals(fileBytes.length, resultado.get().length);
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void deveRetornarOptionalVazioQuandoErroAoLerArquivo() {
        var fileName = "arquivo-inexistente.csv";

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("Arquivo não encontrado"));

        var resultado = s3Bucket.readFileAsBytes(fileName);

        assertFalse(resultado.isPresent());
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void deveResolverLocalizacaoS3ComPrefixo() {
        when(configuration.getS3Url()).thenReturn("https://example-bucket.s3.amazonaws.com/path/to/files");
        var fileName = "books.csv";

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        try {
            when(responseInputStream.readAllBytes()).thenReturn(new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        s3Bucket.readFileAsBytes(fileName);

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void deveResolverLocalizacaoS3SemPrefixo() {
        when(configuration.getS3Url()).thenReturn("https://example-bucket.s3.amazonaws.com");
        var fileName = "books.csv";

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        try {
            when(responseInputStream.readAllBytes()).thenReturn(new byte[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        s3Bucket.readFileAsBytes(fileName);

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }
}

