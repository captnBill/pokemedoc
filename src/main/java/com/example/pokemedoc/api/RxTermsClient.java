package com.example.pokemedoc.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * GET {base}/search?terms=XX  on the NLM Clinical Table Search Service (RxTerms v3).
 *
 * The response is a JSON array: [total, codes, extraFields, displayStrings, codeSystems]
 * where displayStrings is a list of one-element lists, e.g. [["Aspirin (Oral Pill)"], ...].
 */
@Component
public class RxTermsClient {

    private final RestClient rest;
    private final int count;

    public RxTermsClient(RestClient.Builder builder,
                         @Value("${pokemedoc.rxterms.base-url:https://clinicaltables.nlm.nih.gov/api/rxterms/v3}") String baseUrl,
                         @Value("${pokemedoc.rxterms.count:500}") int count) {
        this.rest = builder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Pokemedoc/1.0")
                .build();
        this.count = count;
    }

    /** Raw display strings for a search term (e.g. "Aspirin (Oral Pill)"). */
    public List<String> search(String terms) {
        JsonNode root = rest.get()
                .uri(uri -> {
                    uri.path("/search").queryParam("terms", terms);
                    if (count > 0) {
                        uri.queryParam("count", count);
                    }
                    return uri.build();
                })
                .retrieve()
                .body(JsonNode.class);
        return parseDisplayStrings(root);
    }

    static List<String> parseDisplayStrings(JsonNode root) {
        List<String> out = new ArrayList<>();
        if (root == null || !root.isArray() || root.size() < 4 || !root.get(3).isArray()) {
            return out;
        }
        for (JsonNode entry : root.get(3)) {
            if (entry.isArray()) {
                for (JsonNode s : entry) {
                    if (s.isTextual()) {
                        out.add(s.asText());
                    }
                }
            } else if (entry.isTextual()) {
                out.add(entry.asText());
            }
        }
        return out;
    }
}
