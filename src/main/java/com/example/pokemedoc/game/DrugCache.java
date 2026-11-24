package com.example.pokemedoc.game;

import com.example.pokemedoc.api.RxTermsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Pool of valid drug names, grown lazily.
 *
 * We pick two random letters on every question and call RxTerms. Each such call ("prefix fetch")
 * adds all the usable names it returns to a shared pool, and questions are drawn from that pool:
 * while the pool is small every question grows it, afterwards only a small fraction of questions do.
 * So API traffic falls quickly as the game gets played.
 */
@Component
public class DrugCache {

    private static final Logger log = LoggerFactory.getLogger(DrugCache.class);
    private static final int MAX_PREFIX_ATTEMPTS = 10;

    private final RxTermsClient client;
    private final int minPoolSize;
    private final double refreshChance;

    private final Object lock = new Object();
    private final Set<String> seen = new HashSet<>();
    private final List<String> names = new ArrayList<>();

    public DrugCache(RxTermsClient client,
                     @Value("${pokemedoc.drugs.min-pool-size:200}") int minPoolSize,
                     @Value("${pokemedoc.drugs.refresh-chance:0.05}") double refreshChance) {
        this.client = client;
        this.minPoolSize = minPoolSize;
        this.refreshChance = refreshChance;
    }

    /**
     * A random drug name that {@code excluded} does not reject. If every name in the pool is excluded,
     * new prefixes are fetched until a fresh name turns up (or the attempts run out).
     */
    public String randomName(Predicate<String> excluded) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (size() < minPoolSize || rnd.nextDouble() < refreshChance) {
            fetchRandomPrefix();
        }
        String name = pick(excluded);
        for (int attempt = 1; name == null && attempt < MAX_PREFIX_ATTEMPTS; attempt++) {
            fetchRandomPrefix();
            name = pick(excluded);
        }
        if (name == null) {
            throw new IllegalStateException("No usable drug name could be fetched");
        }
        return name;
    }

    public int size() {
        synchronized (lock) {
            return names.size();
        }
    }

    private String pick(Predicate<String> excluded) {
        synchronized (lock) {
            List<String> candidates = new ArrayList<>();
            for (String n : names) {
                if (!excluded.test(n)) {
                    candidates.add(n);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
    }

    /** Fetches one random two-letter prefix and adds every usable name to the pool. Never throws. */
    private void fetchRandomPrefix() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String prefix = "" + (char) ('A' + rnd.nextInt(26)) + (char) ('A' + rnd.nextInt(26));
        try {
            int added = 0;
            for (String raw : client.search(prefix)) {
                if (raw == null || raw.isEmpty() || raw.equals("0")) {
                    continue;
                }
                String formatted = NameFormatter.formatDrugName(raw);
                if (!NameFormatter.isUsableDrugName(formatted)) {
                    continue;
                }
                synchronized (lock) {
                    if (seen.add(formatted)) {
                        names.add(formatted);
                        added++;
                    }
                }
            }
            log.debug("Prefix {} added {} drug names (pool size {})", prefix, added, size());
        } catch (RestClientException e) {
            log.warn("RxTerms call for prefix {} failed: {}", prefix, e.getMessage());
        }
    }
}
