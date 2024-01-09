package searchengine.dto;

import com.github.demidko.aot.WordformMeaning;


public record LemmaMining(
        String lemma,
        WordformMeaning meaning
) {
}
