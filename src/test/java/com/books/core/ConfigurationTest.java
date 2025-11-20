package com.books.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationTest {

    @Test
    void deveCarregarConfiguracaoComSucesso() {
        var properties = criarPropertiesValidas();
        var inputStream = criarInputStream(properties);
        var originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            var testClassLoader = new TestClassLoader(originalClassLoader, inputStream);
            Thread.currentThread().setContextClassLoader(testClassLoader);

            var configuration = new Configuration();

            assertNotNull(configuration);
            assertEquals("https://example-bucket.s3.amazonaws.com", configuration.getS3Url());
            assertEquals("books", configuration.getBooksTableName());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    void deveLancarExcecaoQuandoArquivoNaoEncontrado() {
        var originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            var testClassLoader = new TestClassLoader(originalClassLoader, null);
            Thread.currentThread().setContextClassLoader(testClassLoader);

            assertThrows(IllegalStateException.class, Configuration::new);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    void deveLancarExcecaoQuandoS3UrlNaoConfigurado() {
        var properties = new Properties();
        properties.setProperty("dynamodb.table.books", "books");
        var inputStream = criarInputStream(properties);
        var originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            var testClassLoader = new TestClassLoader(originalClassLoader, inputStream);
            Thread.currentThread().setContextClassLoader(testClassLoader);

            assertThrows(IllegalStateException.class, Configuration::new);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    void deveLancarExcecaoQuandoBooksTableNameNaoConfigurado() {
        var properties = new Properties();
        properties.setProperty("s3.url", "https://example-bucket.s3.amazonaws.com");
        var inputStream = criarInputStream(properties);
        var originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            var testClassLoader = new TestClassLoader(originalClassLoader, inputStream);
            Thread.currentThread().setContextClassLoader(testClassLoader);

            assertThrows(IllegalStateException.class, Configuration::new);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private Properties criarPropertiesValidas() {
        var properties = new Properties();
        properties.setProperty("s3.url", "https://example-bucket.s3.amazonaws.com");
        properties.setProperty("dynamodb.table.books", "books");
        return properties;
    }

    private InputStream criarInputStream(Properties properties) {
        try {
            var outputStream = new java.io.ByteArrayOutputStream();
            properties.store(outputStream, null);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestClassLoader extends ClassLoader {
        private final ClassLoader parent;
        private final InputStream inputStream;

        TestClassLoader(ClassLoader parent, InputStream inputStream) {
            super(parent);
            this.parent = parent;
            this.inputStream = inputStream;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if ("application.properties".equals(name)) {
                return inputStream;
            }
            return parent.getResourceAsStream(name);
        }
    }
}
