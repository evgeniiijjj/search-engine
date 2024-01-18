package searchengine.dto;

import searchengine.entities.Page;
import searchengine.services.utils.WordFormMeaningSpec;

import java.util.List;


public record PageLemmas(
        Page page,
        List<WordFormMeaningSpec> lemmas,
        float relevance

) implements Comparable<PageLemmas> {

    @Override
    public int compareTo(PageLemmas o) {

        return Integer.compare(o.lemmas.size(), lemmas.size()) * 1000 +
                Float.compare(o.relevance, relevance);
    }
}
