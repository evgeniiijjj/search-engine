package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;


public record SearchResult(
        String site,
        String siteName,
        String uri,
        String title,
        String snippet,
        Float relevance,
        @JsonIgnore
        Integer meaningsCount
) implements Comparable<SearchResult> {

    @Override
    public int compareTo(SearchResult o) {

        return o.meaningsCount.compareTo(meaningsCount) * 1000 +
                o.relevance.compareTo(relevance);
    }
}
