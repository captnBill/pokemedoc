package com.example.pokemedoc.game;

import java.io.Serializable;

/**
 * One round of the quiz. For medicines, {@code types} is empty and {@code spriteUrl} is null.
 * The answer ({@code pokemon}) lives on the server in the HTTP session and is never sent to the browser.
 */
public record Question(boolean pokemon, String name, String types, String spriteUrl) implements Serializable {

    public static Question ofPokemon(Pokemon p) {
        return new Question(true, p.name(), p.types() == null ? "" : p.types(), p.spriteUrl());
    }

    public static Question ofMedicine(String name) {
        return new Question(false, name, "", null);
    }
}
