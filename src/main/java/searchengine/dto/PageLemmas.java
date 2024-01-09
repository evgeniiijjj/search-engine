package searchengine.dto;

import searchengine.entities.Index;
import searchengine.entities.Page;

import java.util.List;
import java.util.Map;


public record PageLemmas(
        Page page,
        List<LemmaMining> lemmas,
        float relevance

) implements Comparable<PageLemmas> {

    public PageLemmas(Map.Entry<Page, List<IndexLemmaMining>> entry) {

        this(
                entry.getKey(),
                entry
                        .getValue()
                        .stream()
                        .map(IndexLemmaMining::meaning)
                        .toList(),
                entry
                        .getValue()
                        .stream()
                        .map(IndexLemmaMining::index)
                        .map(Index::getRank)
                        .reduce(Float::sum)
                        .orElse(0F)
        );
    }

    @Override
    public int compareTo(PageLemmas o) {

        return Integer.compare(o.lemmas.size(), lemmas.size()) * 1000
                + Double.compare(o.relevance, relevance);
    }
}
