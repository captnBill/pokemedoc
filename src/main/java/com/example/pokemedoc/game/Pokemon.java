package com.example.pokemedoc.game;

import java.io.Serializable;

/** A cached Pokémon: display name already formatted, types joined with "/" (e.g. "grass/poison"). */
public record Pokemon(int id, String name, String types, String spriteUrl) implements Serializable {
}
