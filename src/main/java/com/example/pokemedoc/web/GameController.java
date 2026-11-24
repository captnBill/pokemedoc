package com.example.pokemedoc.web;

import com.example.pokemedoc.game.GameState;
import com.example.pokemedoc.game.GameState.Phase;
import com.example.pokemedoc.game.MedicineImageService;
import com.example.pokemedoc.game.QuestionService;
import com.example.pokemedoc.score.ScoreService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * The game lives in the HTTP session; every action is a POST
 * followed by a redirect (so refreshing the page never repeats a guess).
 *
 *   GET  /play/new    start a new game (3 lives, score 0)
 *   GET  /play        show the current question / the revealed answer
 *   POST /play/guess  answer=pokemon|medicine
 *   POST /play/next   next question, or (when out of lives) save the score and go home
 */
@Controller
@RequestMapping("/play")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);
    private static final String SESSION_KEY = "game";

    private final QuestionService questions;
    private final MedicineImageService images;
    private final ScoreService scores;

    public GameController(QuestionService questions, MedicineImageService images, ScoreService scores) {
        this.questions = questions;
        this.images = images;
        this.scores = scores;
    }

    @GetMapping("/new")
    public String newGame(HttpSession session) {
        session.setAttribute(SESSION_KEY, new GameState());
        return "redirect:/play";
    }

    @GetMapping
    public String play(HttpSession session, Model model) {
        GameState game = (GameState) session.getAttribute(SESSION_KEY);
        if (game == null) {
            return "redirect:/play/new";
        }

        boolean fetchError = false;
        if (game.getCurrent() == null) {
            try {
                game.setCurrent(questions.next(game::recentlyAsked));
            } catch (Exception e) {
                log.error("Error fetching random name", e);
                fetchError = true;
            }
        }

        boolean revealed = game.getPhase() == Phase.REVEALED;
        model.addAttribute("game", game);
        model.addAttribute("fetchError", fetchError);
        model.addAttribute("revealed", revealed);
        if (!fetchError) {
            model.addAttribute("descKey", descriptionKey(game));
            model.addAttribute("showSprite", revealed
                    && game.getCurrent().pokemon()
                    && game.getCurrent().spriteUrl() != null);
            if (revealed && !game.getCurrent().pokemon()) {
                // Illustration for the medicine (or a pill icon if none was found), shown after answering
                model.addAttribute("showMedicineArt", true);
                images.find(game.getCurrent().name()).ifPresent(img -> model.addAttribute("medicineImage", img));
            }
            model.addAttribute("pokemonClass", resultClass(game, true));
            model.addAttribute("medicineClass", resultClass(game, false));
        }
        return "quiz";
    }

    @PostMapping("/guess")
    public String guess(@RequestParam String answer, HttpSession session) {
        GameState game = (GameState) session.getAttribute(SESSION_KEY);
        if (game == null) {
            return "redirect:/play/new";
        }
        game.guess("pokemon".equals(answer));   // ignored if already answered
        return "redirect:/play";
    }

    @PostMapping("/next")
    public String next(HttpSession session, Principal principal, RedirectAttributes redirect) {
        GameState game = (GameState) session.getAttribute(SESSION_KEY);
        if (game == null || game.getPhase() != Phase.REVEALED) {
            return "redirect:/play";
        }
        if (game.isGameOver()) {
            // Android: "Next" after Game Over -> name dialog -> save -> back to the main screen.
            scores.save(principal.getName(), game.getScore());
            session.removeAttribute(SESSION_KEY);
            redirect.addFlashAttribute("savedScore", game.getScore());
            return "redirect:/";
        }
        game.advance();
        return "redirect:/play";
    }

    /** Message key for the line under the name (same texts and same "Game Over!" override as the app). */
    private static String descriptionKey(GameState game) {
        if (game.getPhase() == Phase.ASKING) {
            return "question.prompt";
        }
        if (game.isGameOver()) {
            return "game.over";
        }
        String kind = game.getCurrent().pokemon() ? "pokemon" : "medicine";
        return (game.isCorrect() ? "correct." : "incorrect.") + kind;
    }

    /** CSS class for the button the player pressed: green if right, red if wrong. */
    private static String resultClass(GameState game, boolean forPokemonButton) {
        if (game.getPhase() != Phase.REVEALED
                || game.getGuessedPokemon() == null
                || game.getGuessedPokemon() != forPokemonButton) {
            return "";
        }
        return game.isCorrect() ? "correct" : "wrong";
    }
}
