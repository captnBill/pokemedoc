package com.example.pokemedoc.game;

import com.example.pokemedoc.api.CommonsImageClient;
import com.example.pokemedoc.api.WikipediaImageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Illustration lookup: Wikimedia Commons photo search first, then (if nothing convincing was found) the verified
 * Wikipedia article. Results are cached, "no image found" included, so a medicine is looked up at most once.
 * A lookup that failed on the network is not cached as "none" (it is retried next time) and never breaks the game.
 */
@Service
public class MedicineImageService {

    private static final Logger log = LoggerFactory.getLogger(MedicineImageService.class);

    private final CommonsImageClient commons;
    private final WikipediaImageClient wikipedia;
    private final boolean enabled;
    private final boolean wikipediaFallback;
    private final Map<String, Optional<MedicineImage>> cache = new ConcurrentHashMap<>();

    public MedicineImageService(CommonsImageClient commons,
                                WikipediaImageClient wikipedia,
                                @Value("${pokemedoc.images.enabled:true}") boolean enabled,
                                @Value("${pokemedoc.images.wikipedia-fallback:true}") boolean wikipediaFallback) {
        this.commons = commons;
        this.wikipedia = wikipedia;
        this.enabled = enabled;
        this.wikipediaFallback = wikipediaFallback;
    }

    public Optional<MedicineImage> find(String medicineName) {
        if (!enabled) {
            return Optional.empty();
        }
        String key = medicineName.toLowerCase(Locale.ROOT);
        Optional<MedicineImage> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        boolean failed = false;
        String source = "none";
        Optional<MedicineImage> found = Optional.empty();

        try {
            found = commons.find(medicineName);
            if (found.isPresent()) {
                source = "Wikimedia Commons";
            }
        } catch (RuntimeException e) {   // never let an image problem break the quiz
            failed = true;
            log.warn("Commons image lookup for '{}' failed: {}", medicineName, e.toString());
        }

        if (found.isEmpty() && wikipediaFallback) {
            try {
                found = wikipedia.find(medicineName);
                if (found.isPresent()) {
                    source = found.get().diagram() ? "Wikipedia (structure diagram, last resort)" : "Wikipedia";
                }
            } catch (RuntimeException e) {
                failed = true;
                log.warn("Wikipedia image lookup for '{}' failed: {}", medicineName, e.toString());
            }
        }

        log.info("Image for '{}': {}{}", medicineName, source, (found.isEmpty() && failed) ? " (a lookup failed)" : "");
        if (found.isPresent() || !failed) {
            cache.put(key, found);
        }
        return found;
    }
}
