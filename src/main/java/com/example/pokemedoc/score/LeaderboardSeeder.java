package com.example.pokemedoc.score;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** "Nurse Miriam" (100) is inserted when the leaderboard is empty. */
@Component
public class LeaderboardSeeder implements ApplicationRunner {

    private final ScoreRepository repo;

    public LeaderboardSeeder(ScoreRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() == 0) {
            repo.save(new Score("Nurse Miriam", 100, LocalDate.of(2026, 11, 7)));
        }
    }
}
