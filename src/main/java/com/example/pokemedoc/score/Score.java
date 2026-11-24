package com.example.pokemedoc.score;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int score;

    @Column(name = "played_on", nullable = false)
    private LocalDate playedOn;

    protected Score() {
    }

    public Score(String username, int score, LocalDate playedOn) {
        this.username = username;
        this.score = score;
        this.playedOn = playedOn;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public LocalDate getPlayedOn() { return playedOn; }
}
