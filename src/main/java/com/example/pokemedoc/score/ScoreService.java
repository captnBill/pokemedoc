package com.example.pokemedoc.score;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScoreService {

    private final ScoreRepository repo;

    public ScoreService(ScoreRepository repo) {
        this.repo = repo;
    }

    public void save(String username, int score) {
        repo.save(new Score(username, score, LocalDate.now()));
    }

    public List<Score> leaderboard() {
        return repo.findTop50ByOrderByScoreDescIdAsc();
    }
}
