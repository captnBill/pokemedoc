package com.example.pokemedoc.api;

import com.example.pokemedoc.api.CommonsImagePicker.Candidate;
import com.example.pokemedoc.api.WikipediaImageClient.Article;
import com.example.pokemedoc.game.MedicineImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikipediaImageClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Article article(String pageJson) throws Exception {
        return WikipediaImageClient.parseArticle(mapper.readTree("{\"query\":{\"pages\":[" + pageJson + "]}}"));
    }

    @Test
    void parsesAnArticleAboutAMedicine() throws Exception {
        Article a = article("""
                {"pageid":1,"title":"Ibuprofen",
                 "extract":"Ibuprofen is a nonsteroidal anti-inflammatory drug (NSAID) used to relieve pain.",
                 "categories":[{"title":"Category:Analgesics"}],
                 "thumbnail":{"source":"https://upload.wikimedia.org/x.png","width":320,"height":240},
                 "pageimage":"Ibuprofen.jpg"}""");
        assertNotNull(a);
        assertEquals("Ibuprofen", a.title());
        assertTrue(WikipediaImageClient.isAboutMedicine(a));
    }

    @Test
    void missingAndDisambiguationPagesAreIgnored() throws Exception {
        assertNull(article("{\"title\":\"Nope\",\"missing\":true}"));
        assertNull(article("{\"title\":\"Mercury\",\"pageprops\":{\"disambiguation\":\"\"}}"));
        assertNull(WikipediaImageClient.parseArticle(null));
    }

    @Test
    void anArticleAboutSomethingElseIsNotAccepted() throws Exception {
        Article planet = article("""
                {"title":"Mercury","extract":"Mercury is the first planet from the Sun and the smallest in the Solar System.",
                 "categories":[{"title":"Category:Planets of the Solar System"}]}""");
        assertFalse(WikipediaImageClient.isAboutMedicine(planet));
    }

    @Test
    void aDrugCategoryAloneCanVerifyAnArticle() throws Exception {
        Article a = article("""
                {"title":"Xyz","extract":"Xyz is a compound.","categories":[{"title":"Category:Antihistamines"}]}""");
        assertTrue(WikipediaImageClient.isAboutMedicine(a));
    }

    @Test
    void theLeadImageIsUsedOnlyWhenItIsNotAMoleculeOrDiagram() throws Exception {
        String thumb = "\"thumbnail\":{\"source\":\"https://upload.wikimedia.org/x.png\",\"width\":320,\"height\":240}";

        Optional<MedicineImage> photo = WikipediaImageClient.leadImage(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin pills.jpg\"}"));
        assertTrue(photo.isPresent());
        assertEquals("https://en.wikipedia.org/wiki/File:Aspirin%20pills.jpg", photo.get().pageUrl());

        assertTrue(WikipediaImageClient.leadImage(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin-skeletal.svg\"}")).isEmpty());
        assertTrue(WikipediaImageClient.leadImage(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin-B-3D-balls.png\"}")).isEmpty());
        assertTrue(WikipediaImageClient.leadImage(article("{\"title\":\"Aspirin\"}")).isEmpty());
    }

    @Test
    void otherArticleImagesNeedAMedicineWordInTheirName() {
        Candidate logo = new Candidate("File:Bayer logo.png", 0, "https://upload.wikimedia.org/a.png",
                "https://commons.wikimedia.org/wiki/File:Bayer_logo.png", "image/png", 400, 400, List.of());
        Candidate unrelated = new Candidate("File:Willow tree.jpg", 0, "https://upload.wikimedia.org/b.jpg",
                "https://commons.wikimedia.org/wiki/File:Willow_tree.jpg", "image/jpeg", 800, 600, List.of());
        Candidate tablets = new Candidate("File:Aspirin tablets.jpg", 0, "https://upload.wikimedia.org/c.jpg",
                "https://commons.wikimedia.org/wiki/File:Aspirin_tablets.jpg", "image/jpeg", 800, 600, List.of());

        assertTrue(CommonsImagePicker.pickFromArticle(List.of(logo, unrelated)).isEmpty());
        Optional<MedicineImage> picked = CommonsImagePicker.pickFromArticle(List.of(logo, unrelated, tablets));
        assertTrue(picked.isPresent());
        assertTrue(picked.get().url().endsWith("c.jpg"));
    }

    @Test
    void aLeadMoleculeDrawingIsOnlyAcceptedAsALastResortAndFlaggedAsDiagram() throws Exception {
        String thumb = "\"thumbnail\":{\"source\":\"https://upload.wikimedia.org/x.png\",\"width\":320,\"height\":150}";

        Optional<MedicineImage> svg = WikipediaImageClient.moleculeLead(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin-skeletal.svg\"}"));
        assertTrue(svg.isPresent());
        assertTrue(svg.get().diagram());

        assertTrue(WikipediaImageClient.moleculeLead(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin-B-3D-balls.png\"}")).isPresent());
        // a photo is not a molecule drawing, logos and reaction schemes are never acceptable
        assertTrue(WikipediaImageClient.moleculeLead(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin pills.jpg\"}")).isEmpty());
        assertTrue(WikipediaImageClient.moleculeLead(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Bayer logo.svg\"}")).isEmpty());
        assertTrue(WikipediaImageClient.moleculeLead(article(
                "{\"title\":\"Aspirin\"," + thumb + ",\"pageimage\":\"Aspirin synthesis.png\"}")).isEmpty());
    }
}
