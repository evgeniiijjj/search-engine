package searchengine.dto;

import com.github.demidko.aot.WordformMeaning;
import searchengine.entities.Page;

import java.util.List;


public record PageLemmas(
        Page page,
        List<WordformMeaning> lemmas,
        float relevance

) implements Comparable<PageLemmas> {

    @Override
    public int compareTo(PageLemmas o) {

        return Integer.compare(o.lemmas.size(), lemmas.size()) * 1000 +
                Float.compare(o.relevance, relevance);
    }
}
