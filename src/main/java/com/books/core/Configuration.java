package com.books.core;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@Getter
@Slf4j
public final class Configuration {

    private static final String CONFIG_FILE = "application.yaml";
    private static final String S3_SECTION = "s3";
    private static final String S3_URL_KEY = "url";

    private final S3Configuration s3Configuration;

    private Configuration(S3Configuration s3Configuration) {
        this.s3Configuration = s3Configuration;
    }

    public static Configuration load() {
        var loaderOptions = new LoaderOptions();
        var yaml = new Yaml(new SafeConstructor(loaderOptions));

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Arquivo application.yaml não encontrado no classpath");
            }

            var rawConfig = yaml.<Map<String, Object>>load(inputStream);
            var configMap = Optional.ofNullable(rawConfig).map(Configuration::toFlatStringKeyMap).orElseGet(Map::of);

            var s3ConfigMap = Optional.ofNullable(configMap.get(S3_SECTION)).map(Configuration::toFlatStringKeyMap)
                    .orElseGet(Map::of);
            var s3Url = Optional.ofNullable(s3ConfigMap.get(S3_URL_KEY))
                    .map(Object::toString)
                    .filter(url -> !url.isBlank())
                    .orElseThrow(
                            () -> new IllegalStateException("Variável 's3.url' não configurada no application.yaml"));

            var s3Configuration = new S3Configuration(s3Url);
            return new Configuration(s3Configuration);
        } catch (Exception ex) {
            log.error("Erro ao carregar configuracoes do arquivo {}", CONFIG_FILE, ex);
            throw new IllegalStateException("Não foi possível carregar as configurações da aplicação", ex);
        }
    }

    public record S3Configuration(String url) {
    }

    private static Map<String, Object> toFlatStringKeyMap(Object value) {
        if (value instanceof Map<?, ?> entries) {
            return entries.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
        }
        return Map.of();
    }
}
