package com.example.pokemedoc.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RxTermsClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsTheDisplayStringsFromIndexThree() throws Exception {
        String json = "[2,[\"1\",\"2\"],null,[[\"Aspirin (Oral Pill)\"],[\"Abacavir (Oral Pill)\"]]]";
        List<String> names = RxTermsClient.parseDisplayStrings(mapper.readTree(json));
        assertEquals(List.of("Aspirin (Oral Pill)", "Abacavir (Oral Pill)"), names);
    }

    @Test
    void emptyOrMalformedResponsesGiveNoNames() throws Exception {
        assertTrue(RxTermsClient.parseDisplayStrings(mapper.readTree("[0,[],null,[]]")).isEmpty());
        assertTrue(RxTermsClient.parseDisplayStrings(mapper.readTree("{}")).isEmpty());
        assertTrue(RxTermsClient.parseDisplayStrings(null).isEmpty());
    }
}
