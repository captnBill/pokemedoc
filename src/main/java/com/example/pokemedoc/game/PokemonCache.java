package com.example.pokemedoc.game;

import com.example.pokemedoc.api.PokeApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Picks a random Pokémon (ids 1..1025) and remembers every one it has already
 * fetched, so PokeAPI is only called the first time a given Pokémon is drawn.
 */
@Component
public class PokemonCache {

    private static final Logger log = LoggerFactory.getLogger(PokemonCache.class);
    /** There are 1025 Pokémon. */
    static final int MAX_ID = 1025;
    private static final int MAX_ATTEMPTS = 30;

    private final PokeApiClient client;
    private final Map<Integer, Pokemon> cache = new ConcurrentHashMap<>();

    public PokemonCache(PokeApiClient client) {
        this.client = client;
    }

    /** A random Pokémon whose (formatted) name is not rejected by {@code excluded}. */
    public Pokemon random(Predicate<String> excluded) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Pokemon p = randomAny();
            if (!excluded.test(p.name())) {
                return p;
            }
        }
        throw new IllegalStateException("Could not find a Pokemon that was not asked recently");
    }

    private Pokemon randomAny() {
        int id = ThreadLocalRandom.current().nextInt(1, MAX_ID + 1);
        Pokemon cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        try {
            Pokemon fetched = client.fetch(id);
            cache.put(id, fetched);
            return fetched;
        } catch (RestClientException e) {
            // API hiccup: serve something we already have rather than failing the round.
            List<Pokemon> known = new ArrayList<>(cache.values());
            if (known.isEmpty()) {
                throw e;
            }
            log.warn("PokeAPI call for id {} failed ({}), serving a cached Pokemon instead", id, e.getMessage());
            return known.get(ThreadLocalRandom.current().nextInt(known.size()));
        }
    }

    public int size() {
        return cache.size();
    }
}
