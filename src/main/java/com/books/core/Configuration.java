package com.books.core;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import jakarta.inject.Singleton;

@Slf4j
@Getter
@Singleton
public final class Configuration {

    private static final String CONFIG_FILE = "application.properties";
    private static final String S3_URL_KEY = "s3.url";
    private static final String DYNAMODB_TABLE_BOOKS_KEY = "dynamodb.table.books";

    private final String s3Url;
    private final String booksTableName;

    public Configuration() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Arquivo application.properties não encontrado no classpath");
            }

            var properties = new Properties();
            properties.load(inputStream);

            var s3Url = Optional.ofNullable(properties.getProperty(S3_URL_KEY))
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .orElseThrow(() -> new IllegalStateException(
                            "Variável 's3.url' não configurada no application.properties"));

            var booksTableName = Optional.ofNullable(properties.getProperty(DYNAMODB_TABLE_BOOKS_KEY))
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .orElseThrow(() -> new IllegalStateException(
                            "Variável 'dynamodb.table.books' não configurada no application.properties"));

            this.s3Url = s3Url;
            this.booksTableName = booksTableName;
        } catch (Exception ex) {
            log.error("Erro ao carregar configuracoes do arquivo {}", CONFIG_FILE, ex);
            throw new IllegalStateException("Não foi possível carregar as configurações da aplicação", ex);
        }
    }
}
