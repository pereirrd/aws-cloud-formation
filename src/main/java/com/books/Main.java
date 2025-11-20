package com.books;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Main {

    public static void main(String[] args) {
        var greeting = "AWS Cloud Formation project initialized";
        System.out.println(greeting);
    }
}
