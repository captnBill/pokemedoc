package com.example.pokemedoc.api;

import com.example.pokemedoc.game.NameFormatter;
import com.example.pokemedoc.game.Pokemon;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

/** GET {base}/pokemon/{id} */
@Component
public class PokeApiClient {

    private final RestClient rest;

    public PokeApiClient(RestClient.Builder builder,
                         @Value("${pokemedoc.pokeapi.base-url:https://pokeapi.co/api/v2}") String baseUrl) {
        this.rest = builder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Pokemedoc/1.0")
                .build();
    }

    public Pokemon fetch(int id) {
        PokemonResponse r = rest.get()
                .uri("/pokemon/{id}", id)
                .retrieve()
                .body(PokemonResponse.class);
        if (r == null || r.name() == null) {
            throw new IllegalStateException("Empty PokeAPI response for id " + id);
        }
        String types = r.types() == null ? "" : r.types().stream()
                .map(t -> t.type().name())
                .collect(Collectors.joining("/"));
        String sprite = r.sprites() == null ? null : r.sprites().frontDefault();
        return new Pokemon(id, NameFormatter.formatPokemonName(r.name()), types, sprite);
    }

    public record PokemonResponse(String name, List<TypeSlot> types, Sprites sprites) {
    }

    public record TypeSlot(int slot, TypeRef type) {
    }

    public record TypeRef(String name) {
    }

    public record Sprites(@JsonProperty("front_default") String frontDefault) {
    }
}
