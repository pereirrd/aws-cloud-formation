package com.books.repository.dynamo;

import com.books.core.Configuration;
import com.books.repository.dynamo.entity.BookEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private Configuration configuration;

    @Mock
    private DynamoDbTable<BookEntity> bookTable;

    private BookRepository bookRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(configuration.getBooksTableName()).thenReturn("books");
        when(dynamoDbEnhancedClient.table(anyString(), any(TableSchema.class)))
                .thenReturn(bookTable);
        bookRepository = new BookRepository(dynamoDbEnhancedClient, configuration);
    }

    @Test
    void deveSalvarLivroComSucesso() {
        var bookEntity = criarBookEntity();

        bookRepository.save(bookEntity);

        verify(bookTable).putItem(bookEntity);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveBuscarLivroPorAutorEGenero() {
        var autor = "Machado de Assis";
        var genero = "Romance";
        var bookEntity = criarBookEntity();

        lenient().when(bookTable.getItem(any(Consumer.class))).thenReturn(bookEntity);

        var resultado = bookRepository.findByAutorAndGenero(autor, genero);

        assertNotNull(resultado);
        assertEquals(Optional.of(bookEntity), resultado);
        verify(bookTable).getItem(any(Consumer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveRetornarOptionalVazioQuandoLivroNaoEncontrado() {
        var autor = "Autor Inexistente";
        var genero = "Gênero Inexistente";

        lenient().when(bookTable.getItem(any(Consumer.class))).thenReturn(null);

        var resultado = bookRepository.findByAutorAndGenero(autor, genero);

        assertEquals(Optional.empty(), resultado);
        verify(bookTable).getItem(any(Consumer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveDeletarLivroPorAutorEGenero() {
        var autor = "Machado de Assis";
        var genero = "Romance";

        bookRepository.delete(autor, genero);

        verify(bookTable).deleteItem(any(Consumer.class));
    }

    private BookEntity criarBookEntity() {
        var bookEntity = new BookEntity();
        bookEntity.setAuthor("Machado de Assis");
        bookEntity.setGenre("Romance");
        bookEntity.setTitle("Dom Casmurro");
        bookEntity.setPeriod("Século XIX");
        return bookEntity;
    }
}

