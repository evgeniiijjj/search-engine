package searchengine.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SearchResult(
        String site,
        String siteName,
        String uri,
        String title,
        String snippet,
        Float relevance,
        @JsonIgnore
        Integer meaningsCount,
        @JsonIgnore
        Integer maxMeaningContinuousSequence
) implements Comparable<SearchResult> {
    @Override
    public int compareTo(SearchResult o) {
        return o.maxMeaningContinuousSequence.compareTo(maxMeaningContinuousSequence) * 10000 +
                o.meaningsCount.compareTo(meaningsCount) * 1000 +
                Integer.compare(o.snippet.length(), snippet.length()) * 100 +
                o.relevance.compareTo(relevance);
    }
}
