package com.books.domain.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvServiceTest {

    private CsvService csvService;

    @BeforeEach
    void setUp() {
        csvService = new CsvService();
    }

    @Test
    void deveParsearCsvComSucesso() {
        var csvContent = "titulo,autor,genero,periodo\n" +
                "Dom Casmurro,Machado de Assis,Romance,Século XIX\n" +
                "O Guarani,José de Alencar,Romance,Século XIX";
        var csvData = csvContent.getBytes();

        var books = csvService.parseCsvToBooks(csvData);

        assertNotNull(books);
        assertEquals(2, books.size());

        var primeiroLivro = books.get(0);
        assertEquals("Dom Casmurro", primeiroLivro.getTitle());
        assertEquals("Machado de Assis", primeiroLivro.getAuthor());
        assertEquals("Romance", primeiroLivro.getGenre());
        assertEquals("Século XIX", primeiroLivro.getPeriod());

        var segundoLivro = books.get(1);
        assertEquals("O Guarani", segundoLivro.getTitle());
        assertEquals("José de Alencar", segundoLivro.getAuthor());
        assertEquals("Romance", segundoLivro.getGenre());
        assertEquals("Século XIX", segundoLivro.getPeriod());
    }

    @Test
    void deveParsearCsvComUmLivro() {
        var csvContent = "titulo,autor,genero,periodo\n" +
                "Memórias Póstumas de Brás Cubas,Machado de Assis,Romance,Século XIX";
        var csvData = csvContent.getBytes();

        var books = csvService.parseCsvToBooks(csvData);

        assertNotNull(books);
        assertEquals(1, books.size());
        assertEquals("Memórias Póstumas de Brás Cubas", books.get(0).getTitle());
        assertEquals("Machado de Assis", books.get(0).getAuthor());
    }

    @Test
    void deveParsearCsvComEspacosEmBranco() {
        var csvContent = "titulo,autor,genero,periodo\n" +
                "  Dom Casmurro  ,  Machado de Assis  ,  Romance  ,  Século XIX  ";
        var csvData = csvContent.getBytes();

        var books = csvService.parseCsvToBooks(csvData);

        assertNotNull(books);
        assertEquals(1, books.size());
        assertEquals("Dom Casmurro", books.get(0).getTitle().trim());
        assertEquals("Machado de Assis", books.get(0).getAuthor().trim());
    }

    @Test
    void deveRetornarListaVaziaQuandoCsvSoTemCabecalho() {
        var csvContent = "titulo,autor,genero,periodo";
        var csvData = csvContent.getBytes();

        var books = csvService.parseCsvToBooks(csvData);

        assertNotNull(books);
        assertTrue(books.isEmpty());
    }
}

