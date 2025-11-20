package com.books.domain.book;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Book {

    @CsvBindByName(column = "titulo")
    private String title;

    @CsvBindByName(column = "autor")
    private String author;

    @CsvBindByName(column = "genero")
    private String genre;

    @CsvBindByName(column = "periodo")
    private String period;
}
