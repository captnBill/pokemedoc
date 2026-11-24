package com.example.pokemedoc.game;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** State of one game (3 lives, score = number of correct guesses). Stored in the HTTP session. */
public class GameState implements Serializable {

    public enum Phase { ASKING, REVEALED }

    public static final int STARTING_LIVES = 3;

    /** A name that was asked can only come back once this many other questions have been asked. */
    public static final int NO_REPEAT_WINDOW = 100;

    private int score = 0;
    private int lives = STARTING_LIVES;
    private Question current;            // null until the next question has been fetched
    private Phase phase = Phase.ASKING;
    private Boolean guessedPokemon;      // which button the player pressed (null while asking)
    private boolean correct;

    /** Lower-cased names of the last {@link #NO_REPEAT_WINDOW} questions, oldest first. */
    private final LinkedHashSet<String> recentNames = new LinkedHashSet<>();

    /** Applies a guess; ignored unless a question is waiting for an answer. */
    public void guess(boolean userSelectedPokemon) {
        if (phase != Phase.ASKING || current == null) {
            return;
        }
        guessedPokemon = userSelectedPokemon;
        correct = (userSelectedPokemon == current.pokemon());
        if (correct) {
            score++;
        } else {
            lives--;
        }
        phase = Phase.REVEALED;
    }

    /** Back to "waiting for the next question" (the "Next" button). */
    public void advance() {
        current = null;
        guessedPokemon = null;
        correct = false;
        phase = Phase.ASKING;
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    /** True if this name (Pokémon or medicine, case-insensitive) is among the last 100 questions of this game. */
    public boolean recentlyAsked(String name) {
        return recentNames.contains(key(name));
    }

    private void remember(String name) {
        String k = key(name);
        recentNames.remove(k);   // keeps insertion order == order of asking
        recentNames.add(k);
        Iterator<String> oldest = recentNames.iterator();
        while (recentNames.size() > NO_REPEAT_WINDOW && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public int getScore() { return score; }
    public int getLives() { return lives; }
    public Question getCurrent() { return current; }
    public Phase getPhase() { return phase; }
    public Boolean getGuessedPokemon() { return guessedPokemon; }
    public boolean isCorrect() { return correct; }

    /** Setting a question also records its name in the no-repeat window. */
    public void setCurrent(Question current) {
        this.current = current;
        if (current != null) {
            remember(current.name());
        }
    }
}
