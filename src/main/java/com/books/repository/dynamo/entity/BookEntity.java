package com.books.repository.dynamo.entity;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@DynamoDbBean
public class BookEntity {

    @Getter(onMethod_ = { @DynamoDbPartitionKey, @DynamoDbAttribute("autor") })
    private String autor;

    @Getter(onMethod_ = { @DynamoDbSortKey, @DynamoDbAttribute("genero") })
    private String genero;
}
