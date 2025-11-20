package com.books.application;

import com.books.domain.book.Book;
import com.books.domain.book.BookMapper;
import com.books.domain.csv.CsvService;
import com.books.domain.exception.FileNotFoundException;
import com.books.domain.exception.ProcessingException;
import com.books.repository.dynamo.BookRepository;
import com.books.repository.dynamo.entity.BookEntity;
import com.books.repository.s3.S3Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookApplicationTest {

    @Mock
    private S3Bucket s3Bucket;

    @Mock
    private CsvService csvService;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookApplication bookApplication;

    private String fileName;
    private byte[] csvData;
    private List<Book> books;
    private BookEntity bookEntity;

    @BeforeEach
    void setUp() {
        fileName = "books.csv";
        csvData = "titulo,autor,genero,periodo\nDom Casmurro,Machado de Assis,Romance,Século XIX".getBytes();

        var book = new Book();
        book.setTitle("Dom Casmurro");
        book.setAuthor("Machado de Assis");
        book.setGenre("Romance");
        book.setPeriod("Século XIX");
        books = List.of(book);

        bookEntity = new BookEntity();
        bookEntity.setTitle("Dom Casmurro");
        bookEntity.setAuthor("Machado de Assis");
        bookEntity.setGenre("Romance");
        bookEntity.setPeriod("Século XIX");
    }

    @Test
    void deveProcessarArquivoCsvComSucesso() {
        when(s3Bucket.readFileAsBytes(fileName)).thenReturn(Optional.of(csvData));
        when(csvService.parseCsvToBooks(csvData)).thenReturn(books);
        when(bookMapper.toEntity(any(Book.class))).thenReturn(bookEntity);

        bookApplication.processCsvFile(fileName);

        verify(s3Bucket).readFileAsBytes(fileName);
        verify(csvService).parseCsvToBooks(csvData);
        verify(bookMapper).toEntity(books.get(0));
        verify(bookRepository).save(bookEntity);
    }

    @Test
    void deveProcessarMultiplosLivrosDoCsv() {
        var book2 = new Book();
        book2.setTitle("O Guarani");
        book2.setAuthor("José de Alencar");
        book2.setGenre("Romance");
        book2.setPeriod("Século XIX");
        var booksList = List.of(books.get(0), book2);

        var entity2 = new BookEntity();
        entity2.setTitle("O Guarani");
        entity2.setAuthor("José de Alencar");
        entity2.setGenre("Romance");
        entity2.setPeriod("Século XIX");

        when(s3Bucket.readFileAsBytes(fileName)).thenReturn(Optional.of(csvData));
        when(csvService.parseCsvToBooks(csvData)).thenReturn(booksList);
        when(bookMapper.toEntity(books.get(0))).thenReturn(bookEntity);
        when(bookMapper.toEntity(book2)).thenReturn(entity2);

        bookApplication.processCsvFile(fileName);

        verify(bookRepository, times(2)).save(any(BookEntity.class));
    }

    @Test
    void deveLancarExcecaoQuandoArquivoNaoEncontradoNoS3() {
        when(s3Bucket.readFileAsBytes(fileName)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> bookApplication.processCsvFile(fileName));

        verify(s3Bucket).readFileAsBytes(fileName);
        verify(csvService, never()).parseCsvToBooks(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoErroAoFazerParsingDoCsv() {
        when(s3Bucket.readFileAsBytes(fileName)).thenReturn(Optional.of(csvData));
        when(csvService.parseCsvToBooks(csvData)).thenThrow(new RuntimeException("Erro ao parsear CSV"));

        assertThrows(ProcessingException.class, () -> bookApplication.processCsvFile(fileName));

        verify(s3Bucket).readFileAsBytes(fileName);
        verify(csvService).parseCsvToBooks(csvData);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoErroAoSalvarNoDynamoDB() {
        when(s3Bucket.readFileAsBytes(fileName)).thenReturn(Optional.of(csvData));
        when(csvService.parseCsvToBooks(csvData)).thenReturn(books);
        when(bookMapper.toEntity(any(Book.class))).thenReturn(bookEntity);
        doThrow(new RuntimeException("Erro ao salvar no DynamoDB")).when(bookRepository).save(bookEntity);

        assertThrows(ProcessingException.class, () -> bookApplication.processCsvFile(fileName));

        verify(s3Bucket).readFileAsBytes(fileName);
        verify(csvService).parseCsvToBooks(csvData);
        verify(bookRepository).save(bookEntity);
    }
}
