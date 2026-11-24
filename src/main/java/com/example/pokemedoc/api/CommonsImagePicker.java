package com.example.pokemedoc.api;

import com.example.pokemedoc.game.MedicineImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides which Wikimedia Commons search result (if any) is a believable picture of a medicine.
 *
 * Precision matters more than coverage: showing nothing (a pill icon) is better than showing a wrong picture.
 *
 */
final class CommonsImagePicker {

    private static final Logger log = LoggerFactory.getLogger(CommonsImagePicker.class);

    /** One search result, already reduced to what the picker needs. */
    record Candidate(String title, int index, String thumbUrl, String pageUrl,
                     String mime, int width, int height, List<String> categories) {
    }

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MIN_SIDE = 200;

    /** File-name words of molecule drawings: rejected as a product photo, but allowed as a last resort. */
    private static final String MOLECULE_WORDS =
            "structure|structures|structural|formula|formulae|molecule|molecules|molecular|skeletal|3d|3 d|"
                    + "ball|stick|chemdraw|chemical";

    /** File-name words that are never acceptable (not the product, not its molecule either). */
    private static final String NEVER_WORDS =
            "diagram|synthesis|mechanism|spectrum|crystal|logo|logotype|icon|symbol|map|chart|graph|flag|"
                    + "signature|portrait|screenshot|poster|cartoon|building|headquarters";

    /** Words in a file name that mean "not a picture of the product". */
    private static final Pattern REJECT_TITLE =
            Pattern.compile("\\b(?:" + MOLECULE_WORDS + "|" + NEVER_WORDS + ")\\b");

    private static final Pattern MOLECULE_TITLE = Pattern.compile("\\b(?:" + MOLECULE_WORDS + ")\\b");
    private static final Pattern NEVER_TITLE = Pattern.compile("\\b(?:" + NEVER_WORDS + ")\\b");
    /** The classic 2D drawings are preferred over 3D ball-and-stick models. */
    private static final Pattern PREFERRED_MOLECULE_TITLE =
            Pattern.compile("\\b(?:skeletal|structure|structural|formula|formulae|chemdraw)\\b");

    private static final Set<String> MOLECULE_MIME =
            Set.of("image/svg+xml", "image/png", "image/jpeg", "image/webp", "image/gif");
    private static final int MOLECULE_MIN_WIDTH = 100;

    /** Category names that mean the same. */
    private static final Pattern REJECT_CATEGORY = Pattern.compile("structur|formula|molecul|skeletal|diagram|logo");

    /** Medicine words in a file name. */
    private static final Pattern EVIDENCE_TITLE = Pattern.compile(
            "\\b(?:box|boxes|package|packages|packaging|packet|pack|blister|blisters|bottle|bottles|carton|vial|vials|"
                    + "ampoule|ampoules|syringe|pen|inhaler|tube|sachet|label|tablet|tablets|pill|pills|capsule|capsules|"
                    + "medicine|medicines|medication|drug|drugs|pharma|pharmacy|syrup|cream|ointment|spray|drops|patch|"
                    + "mg|ml|dose)\\b");

    /** Medicine-related category names. */
    private static final Pattern EVIDENCE_CATEGORY = Pattern.compile(
            "medic|drug|pharma|tablet|pill|capsule|packag|antibiotic|analgesic|vaccine|ointment|syrup|inhaler|"
                    + "blister|dosage|prescription");

    /** Nicer-looking "product shot" words, used as a tie-breaker. */
    private static final Pattern PACKAGING_TITLE = Pattern.compile(
            "\\b(?:box|boxes|package|packages|packaging|packet|pack|blister|blisters|bottle|bottles|carton|vial|vials)\\b");

    private CommonsImagePicker() {
    }

    static Optional<MedicineImage> pick(String medicineName, List<Candidate> candidates, int minScore) {
        Pattern nameInTitle = Pattern.compile(
                "\\b" + Pattern.quote(medicineName.toLowerCase(Locale.ROOT)) + "\\b");

        List<Candidate> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparingInt(Candidate::index));   // Commons' own relevance order

        Candidate best = null;
        int bestScore = -1;
        for (Candidate c : ranked) {
            int score = score(c, nameInTitle);
            log.debug("Image candidate for '{}': {} -> {}", medicineName, c.title(),
                    score < 0 ? "rejected" : "score " + score);
            if (score >= minScore && score > bestScore) {   // '>' keeps the better-ranked one on ties
                best = c;
                bestScore = score;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        log.debug("Image chosen for '{}': {}", medicineName, best.title());
        return Optional.of(new MedicineImage(best.thumbUrl(), best.pageUrl()));
    }

    /** Minimum score for a file taken from a verified article (needs a medicine word in the file name). */
    private static final int ARTICLE_MIN_SCORE = 3;

    /**
     * Picks a product-style photo among the files of an article that is already known to be about the medicine.
     * The file name must still contain a medicine word (box, tablets, blister...) and nothing molecule-like.
     */
    static Optional<MedicineImage> pickFromArticle(List<Candidate> candidates) {
        Candidate best = null;
        int bestScore = -1;
        for (Candidate c : candidates) {
            int score = score(c, null);
            log.debug("Article image candidate: {} -> {}", c.title(), score < 0 ? "rejected" : "score " + score);
            if (score >= ARTICLE_MIN_SCORE && score > bestScore) {
                best = c;
                bestScore = score;
            }
        }
        return best == null
                ? Optional.empty()
                : Optional.of(new MedicineImage(best.thumbUrl(), best.pageUrl()));
    }

    /** jpeg/png/webp file whose name does not look like a molecule, diagram, logo... (e.g. an infobox image). */
    static boolean isUsablePhotoFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        boolean photoType = lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp");
        return photoType && !REJECT_TITLE.matcher(normalizeTitle(fileName)).find();
    }

    /**
     * last resort: a chemical structure drawing among the files of an article already known to be about the
     * medicine. Only drawings of the molecule qualify (never logos, maps, charts, reaction diagrams...);
     * classic 2D structures are preferred over 3D models, otherwise the first file wins.
     */
    static Optional<MedicineImage> pickMolecule(List<Candidate> candidates) {
        Candidate best = null;
        int bestRank = 0;
        for (Candidate c : candidates) {
            int rank = moleculeRank(c);
            log.debug("Molecule candidate: {} -> rank {}", c.title(), rank);
            if (rank > bestRank) {
                best = c;
                bestRank = rank;
            }
        }
        return best == null
                ? Optional.empty()
                : Optional.of(new MedicineImage(best.thumbUrl(), best.pageUrl(), true));
    }

    private static int moleculeRank(Candidate c) {
        if (!c.thumbUrl().startsWith("https://") || c.width() < MOLECULE_MIN_WIDTH
                || !MOLECULE_MIME.contains(c.mime())) {
            return 0;
        }
        String title = normalizeTitle(c.title());
        if (NEVER_TITLE.matcher(title).find() || !MOLECULE_TITLE.matcher(title).find()) {
            return 0;
        }
        return PREFERRED_MOLECULE_TITLE.matcher(title).find() ? 2 : 1;
    }

    /** An infobox-style molecule file: svg, or a name with molecule words; never logos, maps, reaction diagrams... */
    static boolean isMoleculeDiagramFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        boolean knownType = lower.endsWith(".svg") || lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif");
        String title = normalizeTitle(fileName);
        return knownType
                && !NEVER_TITLE.matcher(title).find()
                && (lower.endsWith(".svg") || MOLECULE_TITLE.matcher(title).find());
    }

    /** -1 = rejected, otherwise 0..7 (higher = more convincing). */
    static int score(Candidate c, Pattern nameInTitle) {
        if (!ALLOWED_MIME.contains(c.mime())
                || c.width() < MIN_SIDE || c.height() < MIN_SIDE
                || !c.thumbUrl().startsWith("https://")) {
            return -1;
        }
        String title = normalizeTitle(c.title());
        // nameInTitle == null: the files come from an article already verified to be about the medicine
        if ((nameInTitle != null && !nameInTitle.matcher(title).find()) || REJECT_TITLE.matcher(title).find()) {
            return -1;
        }
        List<String> categories = c.categories().stream().map(CommonsImagePicker::normalizeCategory).toList();
        for (String cat : categories) {
            if (REJECT_CATEGORY.matcher(cat).find()) {
                return -1;
            }
        }

        int score = 0;
        if (EVIDENCE_TITLE.matcher(title).find()) {
            score += 3;
        }
        if (categories.stream().anyMatch(cat -> EVIDENCE_CATEGORY.matcher(cat).find())) {
            score += 3;
        }
        if (PACKAGING_TITLE.matcher(title).find()) {
            score += 1;
        }
        return score;
    }

    /** "File:Zyrtec_10-mg.JPG" -> "zyrtec 10 mg jpg" */
    private static String normalizeTitle(String title) {
        String t = title.replaceFirst("(?i)^file:", "");
        return t.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    /** "Category:Aspirin_tablets" -> "aspirin tablets" */
    private static String normalizeCategory(String category) {
        String c = category.replaceFirst("(?i)^category:", "");
        return c.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
