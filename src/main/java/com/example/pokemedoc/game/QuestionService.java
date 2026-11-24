package com.example.pokemedoc.game;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@Service
public class QuestionService {

    private final PokemonCache pokemon;
    private final DrugCache drugs;

    public QuestionService(PokemonCache pokemon, DrugCache drugs) {
        this.pokemon = pokemon;
        this.drugs = drugs;
    }

    /**
     * 50/50 Pokémon or medicine
     *
     * @param excluded names that must not be asked again yet (see {@link GameState#recentlyAsked})
     */
    public Question next(Predicate<String> excluded) {
        if (ThreadLocalRandom.current().nextBoolean()) {
            return Question.ofPokemon(pokemon.random(excluded));
        }
        return Question.ofMedicine(drugs.randomName(excluded));
    }
}
