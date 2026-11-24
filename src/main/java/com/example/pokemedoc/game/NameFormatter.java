package com.example.pokemedoc.game;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Name-formatting / validation rules
 * (PokemonResponse.formatName, ApiUtil.formatDrugName and the checks in getRandomDrugName).
 */
public final class NameFormatter {

    /**
     * Several Pokemon are stored in the API with "-" segments at the end of their names to differentiate special forms
     * examples : Arceus-Flying, Deoxys-Normal, Oricorio-PomPom, Mimikyu-Disguise, etc.
     * those have to be cut out in order to only display the Pokemon's name. However, some other Pokemon have
     * "-" segments that are part of the real name (e.g. "mr-mime", "porygon-z", "iron-bolt" so this is handled here). */
    private static final Set<String> POKEMON_SUFFIX_EXCEPTIONS = Set.of(
            "oh", "o", "z", "tusk", "tail", "bonnet", "mane", "wing", "shocks",
            "moon", "wake", "fire", "bolt", "mime", "rime", "jr",
            "lu","chien","pao","yu",
            "null");

    private static final Pattern PARENTHESES = Pattern.compile("\\s*\\(.*?\\)\\s*");

    private NameFormatter() {
    }

    /** "bulbasaur" -> "Bulbasaur", "deoxys-normal" -> "Deoxys", "mr-mime" -> "Mr-mime". */
    public static String formatPokemonName(String apiName) {
        String[] parts = apiName.split("-");
        String name = (parts.length > 1
                && !POKEMON_SUFFIX_EXCEPTIONS.contains(parts[parts.length - 1])
                && !parts[0].equals("iron"))
                ? parts[0]
                : apiName;
        return capitalizeFirst(name);
    }

    /** "Aspirin (Oral Pill)" -> "Aspirin", "Foo / Bar" -> "Foo". */
    public static String formatDrugName(String drugName) {
        int slash = drugName.indexOf('/');
        String beforeSlash = slash >= 0 ? drugName.substring(0, slash) : drugName;
        String withoutParentheses = PARENTHESES.matcher(beforeSlash).replaceAll("");
        return capitalizeFirst(withoutParentheses.toLowerCase(Locale.ROOT));
    }

    /**
     * The app only keeps single-word names of at most 20 characters without digits.
     */
    public static boolean isUsableDrugName(String formattedName) {
        if (formattedName == null || formattedName.isBlank()) {
            return false;
        }
        for (int i = 0; i < formattedName.length(); i++) {
            if (Character.isDigit(formattedName.charAt(i))) {
                return false;
            }
        }
        return formattedName.split(" ", -1).length <= 1 && formattedName.length() <= 20;
    }

    static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
