package com.books.core;

import jakarta.inject.Singleton;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Singleton
public final class DynamoDbClientFactory {

    public DynamoDbEnhancedClient createEnhancedClient() {
        var dynamoDbClient = DynamoDbClient.create();
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
