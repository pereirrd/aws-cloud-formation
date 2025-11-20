package com.books.repository.dynamo;

import com.books.core.Configuration;
import com.books.repository.dynamo.entity.BookEntity;

import jakarta.inject.Singleton;

import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Singleton
public class BookRepository {

    private final DynamoDbTable<BookEntity> bookTable;

    public BookRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient, Configuration configuration) {
        this.bookTable = dynamoDbEnhancedClient.table(configuration.getBooksTableName(),
                TableSchema.fromBean(BookEntity.class));
    }

    public void save(BookEntity entity) {
        bookTable.putItem(entity);
    }

    public Optional<BookEntity> findByAutorAndGenero(String autor, String genero) {
        var key = Key.builder().partitionValue(autor).sortValue(genero).build();
        var result = bookTable.getItem(r -> r.key(key));
        return Optional.ofNullable(result);
    }

    public void delete(String autor, String genero) {
        var key = Key.builder().partitionValue(autor).sortValue(genero).build();
        bookTable.deleteItem(r -> r.key(key));
    }
}
