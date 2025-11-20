package com.books.domain.book;

import com.books.repository.dynamo.entity.BookEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface BookMapper {

    BookEntity toEntity(Book book);

    Book toDomain(BookEntity entity);
}
