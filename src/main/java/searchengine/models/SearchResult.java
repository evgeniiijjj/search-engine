package searchengine.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SearchResult(
        String site,
        String siteName,
        String uri,
        String title,
        String snippet,
        Float relevance
) implements Comparable<SearchResult> {
    @Override
    public int compareTo(SearchResult o) {
        return o.relevance.compareTo(relevance);
    }
}
