package com.example.pokemedoc.score;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    /** Highest scores first; ties keep submission order. */
    List<Score> findTop50ByOrderByScoreDescIdAsc();
}
