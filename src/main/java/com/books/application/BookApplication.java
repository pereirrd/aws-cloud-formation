package com.books.application;

import com.books.domain.book.Book;
import com.books.domain.book.BookMapper;
import com.books.domain.csv.CsvService;
import com.books.domain.exception.FileNotFoundException;
import com.books.domain.exception.ProcessingException;
import com.books.repository.dynamo.BookRepository;
import com.books.repository.dynamo.entity.BookEntity;
import com.books.repository.s3.S3Bucket;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class BookApplication {

    private final S3Bucket s3Bucket;
    private final CsvService csvService;
    private final BookMapper bookMapper;
    private final BookRepository bookRepository;

    public void processCsvFile(String fileName) {
        log.info("Iniciando processamento do arquivo CSV: {}", fileName);

        try {
            var csvData = readCsvFileFromS3(fileName);
            var books = parseCsvToBooks(csvData);
            saveBooksToDynamo(books);

            log.info("Processamento concluído com sucesso. {} livros processados do arquivo: {}",
                    books.size(), fileName);
        } catch (FileNotFoundException exception) {
            log.error("Arquivo não encontrado: {}", fileName, exception);
            throw exception;
        } catch (ProcessingException exception) {
            log.error("Erro ao processar arquivo: {}", fileName, exception);
            throw exception;
        } catch (Exception exception) {
            log.error("Erro inesperado ao processar arquivo: {}", fileName, exception);
            throw new ProcessingException("Erro inesperado ao processar arquivo: " + fileName, exception);
        }
    }

    private byte[] readCsvFileFromS3(String fileName) {
        log.info("Lendo arquivo do S3: {}", fileName);

        var fileContent = s3Bucket.readFileAsBytes(fileName);

        if (fileContent.isEmpty()) {
            throw new FileNotFoundException("Arquivo não encontrado no S3: " + fileName);
        }

        log.info("Arquivo lido com sucesso do S3: {}", fileName);
        return fileContent.get();
    }

    private List<Book> parseCsvToBooks(byte[] csvData) {
        log.info("Iniciando parsing do CSV para lista de livros");

        try {
            var books = csvService.parseCsvToBooks(csvData);

            if (books.isEmpty()) {
                log.warn("Nenhum livro encontrado no arquivo CSV");
            } else {
                log.info("CSV parseado com sucesso. {} livros encontrados", books.size());
            }

            return books;
        } catch (Exception exception) {
            throw new ProcessingException("Erro ao fazer parsing do CSV", exception);
        }
    }

    private void saveBooksToDynamo(List<Book> books) {
        log.info("Iniciando salvamento de {} livros no DynamoDB", books.size());

        try {
            books.stream()
                    .map(bookMapper::toEntity)
                    .forEach(this::saveBookEntity);

            log.info("Todos os livros foram salvos com sucesso no DynamoDB");
        } catch (Exception exception) {
            throw new ProcessingException("Erro ao salvar livros no DynamoDB", exception);
        }
    }

    private void saveBookEntity(BookEntity entity) {
        try {
            bookRepository.save(entity);
            log.debug("Livro salvo: autor={}, genero={}", entity.getAuthor(), entity.getGenre());
        } catch (Exception exception) {
            log.error("Erro ao salvar livro: autor={}, genero={}",
                    entity.getAuthor(), entity.getGenre(), exception);
            throw new ProcessingException(
                    String.format("Erro ao salvar livro: autor=%s, genero=%s",
                            entity.getAuthor(), entity.getGenre()),
                    exception);
        }
    }
}
