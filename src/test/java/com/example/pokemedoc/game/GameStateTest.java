package com.example.pokemedoc.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    private static GameState withQuestion(boolean pokemon) {
        GameState g = new GameState();
        g.setCurrent(new Question(pokemon, "Name", "", null));
        return g;
    }

    @Test
    void correctGuessScoresAPoint() {
        GameState g = withQuestion(true);
        g.guess(true);
        assertEquals(1, g.getScore());
        assertEquals(3, g.getLives());
        assertTrue(g.isCorrect());
        assertEquals(GameState.Phase.REVEALED, g.getPhase());
    }

    @Test
    void wrongGuessCostsALife() {
        GameState g = withQuestion(true);
        g.guess(false);
        assertEquals(0, g.getScore());
        assertEquals(2, g.getLives());
        assertFalse(g.isCorrect());
    }

    @Test
    void secondGuessOnTheSameQuestionIsIgnored() {
        GameState g = withQuestion(false);
        g.guess(false);
        g.guess(false);
        assertEquals(1, g.getScore());
    }

    @Test
    void threeWrongGuessesEndTheGame() {
        GameState g = new GameState();
        for (int i = 0; i < 3; i++) {
            g.setCurrent(new Question(true, "Name", "", null));
            g.guess(false);
            assertEquals(i == 2, g.isGameOver());
            g.advance();
        }
        assertEquals(0, g.getLives());
    }

    @Test
    void aNameCanOnlyComeBackAfter100OtherQuestions() {
        GameState g = new GameState();
        for (int i = 0; i < GameState.NO_REPEAT_WINDOW; i++) {
            g.setCurrent(new Question(true, "Name" + i, "", null));
        }
        // the last 100 questions (Name0..Name99) are all still blocked
        assertTrue(g.recentlyAsked("Name0"));
        assertTrue(g.recentlyAsked("name99"));   // case-insensitive

        // one more question pushes the oldest one out of the window
        g.setCurrent(new Question(false, "Name100", "", null));
        assertFalse(g.recentlyAsked("Name0"));
        assertTrue(g.recentlyAsked("Name1"));
        assertTrue(g.recentlyAsked("Name100"));
    }
}
