package com.books.domain.csv;

import com.books.domain.book.Book;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import jakarta.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Singleton
public class CsvService {

    public List<Book> parseCsvToBooks(byte[] csvData) {
        var reader = createReader(csvData);
        var csvToBean = buildCsvToBean(reader);
        return parseCsv(csvToBean);
    }

    private InputStreamReader createReader(byte[] csvData) {
        var inputStream = new ByteArrayInputStream(csvData);
        return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }

    private CsvToBean<Book> buildCsvToBean(InputStreamReader reader) {
        return new CsvToBeanBuilder<Book>(reader)
                .withType(Book.class)
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build();
    }

    private List<Book> parseCsv(CsvToBean<Book> csvToBean) {
        return csvToBean.parse();
    }
}
