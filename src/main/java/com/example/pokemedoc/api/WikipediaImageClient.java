package com.example.pokemedoc.api;

import com.example.pokemedoc.game.MedicineImage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fallback source: the Wikipedia article called like the medicine (redirects followed, so brand names often land on
 * the drug's article).
 */
@Component
public class WikipediaImageClient {

    private static final Logger log = LoggerFactory.getLogger(WikipediaImageClient.class);
    private static final String WIKI = "https://en.wikipedia.org/wiki/";
    private static final int MIN_SIDE = 200;

    /** Drug vocabulary expected in the introduction of an article about a medicine. */
    private static final Pattern DRUG_TEXT = Pattern.compile(
            "\\b(?:drug|drugs|medication|medications|medicine|medicines|pharmaceutical|pharmaceuticals|antibiotic|"
                    + "antibiotics|analgesic|antihistamine|antidepressant|antipsychotic|anticonvulsant|anticoagulant|"
                    + "antiviral|antifungal|antipyretic|vaccine|prodrug|opioid|statin|nsaid|antacid|sedative|"
                    + "stimulant|brand name|over-the-counter|prescription)\\b");

    /** Drug-related category names. */
    private static final Pattern DRUG_CATEGORY = Pattern.compile(
            "drug|medic|pharma|antibiotic|analgesic|antihist|antidepress|antipsych|anticonvuls|anticoag|antiviral|"
                    + "antifung|antipyret|vaccine|opioid|nsaid");

    private static final String ARTICLE_QUERY = "/w/api.php?action=query&format=json&formatversion=2&redirects=1"
            + "&prop=pageimages|pageprops|extracts|categories&piprop=thumbnail|name&pithumbsize={size}"
            + "&ppprop=disambiguation&exintro=1&explaintext=1&exsentences=4&cllimit=max&clshow=!hidden&titles={title}";

    private static final String IMAGES_QUERY = "/w/api.php?action=query&format=json&formatversion=2"
            + "&generator=images&gimlimit=50&prop=imageinfo&iiprop=url|mime|size&iiurlwidth={size}&titles={title}";

    private final RestClient rest;
    private final int thumbnailSize;
    private final boolean moleculeFallback;

    public WikipediaImageClient(RestClient.Builder builder,
                                @Value("${pokemedoc.images.wikipedia-base-url:https://en.wikipedia.org}") String baseUrl,
                                @Value("${pokemedoc.images.thumbnail-size:320}") int thumbnailSize,
                                @Value("${pokemedoc.images.molecule-fallback:true}") boolean moleculeFallback,
                                @Value("${pokemedoc.http.user-agent:Pokemedoc/1.0 (hobby project)}") String userAgent) {
        this.rest = builder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
        this.thumbnailSize = thumbnailSize;
        this.moleculeFallback = moleculeFallback;
    }

    public Optional<MedicineImage> find(String medicineName) {
        JsonNode root = rest.get().uri(ARTICLE_QUERY, thumbnailSize, medicineName)
                .retrieve().body(JsonNode.class);

        Article article = parseArticle(root);
        if (article == null) {
            log.debug("Wikipedia: no usable article for '{}'", medicineName);
            return Optional.empty();
        }
        if (!isAboutMedicine(article)) {
            log.debug("Wikipedia: article '{}' is not about a medicine, ignored", article.title());
            return Optional.empty();
        }

        Optional<MedicineImage> lead = leadImage(article);
        if (lead.isPresent()) {
            return lead;
        }

        // Lead image missing or a molecule/diagram: look through the article's other images.
        JsonNode images = rest.get().uri(IMAGES_QUERY, thumbnailSize, article.title())
                .retrieve().body(JsonNode.class);
        List<CommonsImagePicker.Candidate> candidates = CommonsImageClient.parseCandidates(images);
        Optional<MedicineImage> photo = CommonsImagePicker.pickFromArticle(candidates);
        if (photo.isPresent() || !moleculeFallback) {
            return photo;
        }

        // Nothing better exists: a structure drawing of the right medicine is still an illustration.
        Optional<MedicineImage> molecule = moleculeLead(article);
        return molecule.isPresent() ? molecule : CommonsImagePicker.pickMolecule(candidates);
    }

    // ------------------------------------------------------------------ parsing / checks (unit-tested)

    record Article(String title, String extract, List<String> categories,
                   String leadFile, String leadThumb, int leadWidth, int leadHeight) {
    }

    /** The resolved article, or null for missing pages and disambiguation pages. */
    static Article parseArticle(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode pages = root.path("query").path("pages");
        if (!pages.isArray() || pages.size() == 0) {
            return null;
        }
        JsonNode page = pages.get(0);
        if (page.has("missing") || page.path("pageprops").has("disambiguation")) {
            return null;
        }
        List<String> categories = new ArrayList<>();
        for (JsonNode category : page.path("categories")) {
            categories.add(category.path("title").asText(""));
        }
        JsonNode thumb = page.path("thumbnail");
        return new Article(
                page.path("title").asText(""),
                page.path("extract").asText(""),
                categories,
                page.path("pageimage").asText(""),
                thumb.path("source").asText(""),
                thumb.path("width").asInt(0),
                thumb.path("height").asInt(0));
    }

    /** Introduction mentions drug vocabulary, or a category is drug-related. */
    static boolean isAboutMedicine(Article article) {
        if (DRUG_TEXT.matcher(article.extract().toLowerCase(Locale.ROOT)).find()) {
            return true;
        }
        for (String category : article.categories()) {
            String name = category.replaceFirst("(?i)^category:", "").toLowerCase(Locale.ROOT);
            if (DRUG_CATEGORY.matcher(name).find()) {
                return true;
            }
        }
        return false;
    }

    /** last resort: the lead image if it is a chemical structure drawing (the usual drug infobox image). */
    static Optional<MedicineImage> moleculeLead(Article article) {
        if (article.leadFile().isBlank()
                || !article.leadThumb().startsWith("https://")
                || article.leadWidth() < 100
                || !CommonsImagePicker.isMoleculeDiagramFile(article.leadFile())) {
            return Optional.empty();
        }
        String pageUrl = WIKI + "File:" + UriUtils.encodePathSegment(article.leadFile(), StandardCharsets.UTF_8);
        return Optional.of(new MedicineImage(article.leadThumb(), pageUrl, true));
    }

    /** The article's lead image, unless it is missing, tiny, not a photo-type file or looks like a molecule/diagram. */
    static Optional<MedicineImage> leadImage(Article article) {
        if (article.leadFile().isBlank()
                || !article.leadThumb().startsWith("https://")
                || article.leadWidth() < MIN_SIDE || article.leadHeight() < MIN_SIDE
                || !CommonsImagePicker.isUsablePhotoFile(article.leadFile())) {
            return Optional.empty();
        }
        String pageUrl = WIKI + "File:" + UriUtils.encodePathSegment(article.leadFile(), StandardCharsets.UTF_8);
        return Optional.of(new MedicineImage(article.leadThumb(), pageUrl));
    }
}
