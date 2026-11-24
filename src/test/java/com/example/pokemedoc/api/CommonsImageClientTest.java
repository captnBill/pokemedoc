package com.example.pokemedoc.api;

import com.example.pokemedoc.api.CommonsImagePicker.Candidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonsImageClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesSearchResultsWithImageInfoAndCategories() throws Exception {
        String json = """
                {"query":{"pages":[
                  {"pageid":1,"ns":6,"title":"File:Zyrtec box.jpg","index":2,
                   "imageinfo":[{"thumburl":"https://upload.wikimedia.org/t/320px-Zyrtec_box.jpg","width":1200,"height":900,
                                 "mime":"image/jpeg","descriptionurl":"https://commons.wikimedia.org/wiki/File:Zyrtec_box.jpg"}],
                   "categories":[{"ns":14,"title":"Category:Cetirizine"},{"ns":14,"title":"Category:Medications"}]},
                  {"pageid":2,"ns":6,"title":"File:No info.jpg","index":1}
                ]}}
                """;
        List<Candidate> candidates = CommonsImageClient.parseCandidates(mapper.readTree(json));
        assertEquals(1, candidates.size());   // the page without imageinfo is skipped
        Candidate c = candidates.get(0);
        assertEquals("File:Zyrtec box.jpg", c.title());
        assertEquals(2, c.index());
        assertEquals("https://commons.wikimedia.org/wiki/File:Zyrtec_box.jpg", c.pageUrl());
        assertEquals(List.of("Category:Cetirizine", "Category:Medications"), c.categories());
        assertEquals(1200, c.width());
    }

    @Test
    void emptyOrMalformedResponsesGiveNoCandidates() throws Exception {
        assertTrue(CommonsImageClient.parseCandidates(mapper.readTree("{\"batchcomplete\":true}")).isEmpty());
        assertTrue(CommonsImageClient.parseCandidates(null).isEmpty());
    }
}
