package com.example.pokemedoc.game;

import com.example.pokemedoc.api.RxTermsClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DrugCacheTest {

    private static DrugCache cacheWith(String... rawNames) {
        RxTermsClient client = Mockito.mock(RxTermsClient.class);
        Mockito.when(client.search(Mockito.anyString())).thenReturn(List.of(rawNames));
        return new DrugCache(client, 0, 0.0);
    }

    @Test
    void neverReturnsAnExcludedName() {
        DrugCache cache = cacheWith("Aspirin (Oral Pill)", "Ibuprofen (Oral Pill)");
        for (int i = 0; i < 50; i++) {
            assertEquals("Ibuprofen", cache.randomName(n -> n.equals("Aspirin")));
        }
    }

    @Test
    void failsInsteadOfRepeatingWhenEveryNameIsExcluded() {
        DrugCache cache = cacheWith("Aspirin (Oral Pill)");
        assertThrows(IllegalStateException.class, () -> cache.randomName(n -> true));
    }

    @Test
    void unusableNamesAreNeverPooled() {
        DrugCache cache = cacheWith("Vitamin C (Oral Pill)", "B12 (Oral Pill)", "Aspirin (Oral Pill)");
        for (int i = 0; i < 20; i++) {
            assertEquals("Aspirin", cache.randomName(n -> false));
        }
    }
}
