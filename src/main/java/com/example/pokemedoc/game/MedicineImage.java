package com.example.pokemedoc.game;

import java.io.Serializable;

/**
 * An illustration for a medicine: the thumbnail to show and the page (file description) to credit.
 * {@code diagram} is true for the last-resort chemical structure drawings.
 */
public record MedicineImage(String url, String pageUrl, boolean diagram) implements Serializable {

    public MedicineImage(String url, String pageUrl) {
        this(url, pageUrl, false);
    }
}
