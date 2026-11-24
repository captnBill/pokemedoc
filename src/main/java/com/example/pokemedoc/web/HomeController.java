package com.example.pokemedoc.web;

import com.example.pokemedoc.score.ScoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ScoreService scores;

    public HomeController(ScoreService scores) {
        this.scores = scores;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("scores", scores.leaderboard());
        return "leaderboard";
    }

    @GetMapping("/preferences")
    public String preferences() {
        return "preferences";
    }

    @GetMapping("/credits")
    public String credits() {
        return "credits";
    }
}
