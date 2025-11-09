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

    private final String s3Url;

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

            this.s3Url = s3Url;
        } catch (Exception ex) {
            log.error("Erro ao carregar configuracoes do arquivo {}", CONFIG_FILE, ex);
            throw new IllegalStateException("Não foi possível carregar as configurações da aplicação", ex);
        }
    }
}
