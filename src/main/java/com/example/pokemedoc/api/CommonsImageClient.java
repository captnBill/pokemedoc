package com.example.pokemedoc.api;

import com.example.pokemedoc.api.CommonsImagePicker.Candidate;
import com.example.pokemedoc.game.MedicineImage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Searches Wikimedia Commons for a photo of a medicine.
 *
 * One request returns up to {@code search-limit} files whose NAME contains the medicine name,
 * together with their categories. {@link CommonsImagePicker} then keeps only a
 * result that really looks like a picture of that medicine.
 *
 *   GET /w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=intitle:"Zyrtec" filetype:bitmap ...
 *       &prop=imageinfo|categories&iiprop=url|mime|size&iiurlwidth=320&cllimit=max
 */
@Component
public class CommonsImageClient {

    private final RestClient rest;
    private final int thumbnailSize;
    private final int searchLimit;
    private final String queryTemplate;
    private final int minScore;

    public CommonsImageClient(RestClient.Builder builder,
                              @Value("${pokemedoc.images.base-url:https://commons.wikimedia.org}") String baseUrl,
                              @Value("${pokemedoc.images.thumbnail-size:320}") int thumbnailSize,
                              @Value("${pokemedoc.images.search-limit:30}") int searchLimit,
                              @Value("${pokemedoc.images.query-template:intitle:\"{name}\" filetype:bitmap}") String queryTemplate,
                              @Value("${pokemedoc.images.min-score:3}") int minScore,
                              @Value("${pokemedoc.http.user-agent:Pokemedoc/1.0 (hobby project)}") String userAgent) {
        // Wikimedia asks API clients to send a descriptive User-Agent (ideally with contact info).
        this.rest = builder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
        this.thumbnailSize = thumbnailSize;
        this.searchLimit = searchLimit;
        this.queryTemplate = queryTemplate;
        this.minScore = minScore;
    }

    public Optional<MedicineImage> find(String medicineName) {
        String query = queryTemplate.replace("{name}", medicineName);
        JsonNode root = rest.get()
                .uri("/w/api.php?action=query&format=json&formatversion=2"
                        + "&generator=search&gsrnamespace=6&gsrlimit={limit}&gsrsearch={q}"
                        + "&prop=imageinfo|categories&iiprop=url|mime|size&iiurlwidth={size}&cllimit=max",
                        searchLimit, query, thumbnailSize)
                .retrieve()
                .body(JsonNode.class);
        return CommonsImagePicker.pick(medicineName, parseCandidates(root), minScore);
    }

    static List<Candidate> parseCandidates(JsonNode root) {
        List<Candidate> out = new ArrayList<>();
        JsonNode pages = root == null ? null : root.path("query").path("pages");
        if (pages == null || !pages.isArray()) {
            return out;
        }
        for (JsonNode page : pages) {
            JsonNode infos = page.path("imageinfo");
            if (!infos.isArray() || infos.size() == 0) {
                continue;
            }
            JsonNode info = infos.get(0);
            String thumb = info.path("thumburl").asText("");
            if (thumb.isBlank()) {
                continue;
            }
            String title = page.path("title").asText("");
            String pageUrl = info.path("descriptionurl").asText("");
            if (!pageUrl.startsWith("https://")) {
                pageUrl = "https://commons.wikimedia.org/wiki/"
                        + UriUtils.encodePathSegment(title.replace(' ', '_'), StandardCharsets.UTF_8);
            }
            List<String> categories = new ArrayList<>();
            for (JsonNode category : page.path("categories")) {
                categories.add(category.path("title").asText(""));
            }
            out.add(new Candidate(title, page.path("index").asInt(Integer.MAX_VALUE), thumb, pageUrl,
                    info.path("mime").asText(""), info.path("width").asInt(0), info.path("height").asInt(0),
                    categories));
        }
        return out;
    }
}
