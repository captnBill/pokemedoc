package com.example.pokemedoc.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameFormatterTest {

    @Test
    void pokemonNamesKeepOnlyTheBaseFormUnlessTheSuffixIsPartOfTheName() {
        assertEquals("Bulbasaur", NameFormatter.formatPokemonName("bulbasaur"));
        assertEquals("Deoxys", NameFormatter.formatPokemonName("deoxys-normal"));
        assertEquals("Mr-mime", NameFormatter.formatPokemonName("mr-mime"));
        assertEquals("Porygon-z", NameFormatter.formatPokemonName("porygon-z"));
        assertEquals("Iron-bolt", NameFormatter.formatPokemonName("iron-bolt"));
        assertEquals("Iron-valiant", NameFormatter.formatPokemonName("iron-valiant"));
        assertEquals("Ho-oh", NameFormatter.formatPokemonName("ho-oh"));
    }

    @Test
    void drugNamesLoseParenthesesAndAnythingAfterASlash() {
        assertEquals("Aspirin", NameFormatter.formatDrugName("Aspirin (Oral Pill)"));
        // Faithful to the Android app: the space before "/" is kept, so such names are later rejected
        // by isUsableDrugName (they look like two words).
        assertEquals("Foo ", NameFormatter.formatDrugName("FOO / Bar (Injectable)"));
        assertFalse(NameFormatter.isUsableDrugName("Foo "));
        assertEquals("Ibuprofen", NameFormatter.formatDrugName("ibuprofen"));
    }

    @Test
    void onlyShortSingleWordNamesWithoutDigitsAreUsable() {
        assertTrue(NameFormatter.isUsableDrugName("Aspirin"));
        assertFalse(NameFormatter.isUsableDrugName("Vitamin c"));
        assertFalse(NameFormatter.isUsableDrugName("B12"));
        assertFalse(NameFormatter.isUsableDrugName("Abcdefghijklmnopqrstu"));   // 21 chars
        assertTrue(NameFormatter.isUsableDrugName("Abcdefghijklmnopqrst"));     // 20 chars
        assertFalse(NameFormatter.isUsableDrugName(""));
    }
}
