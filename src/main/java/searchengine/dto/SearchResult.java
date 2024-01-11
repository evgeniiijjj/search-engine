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
        Integer meaningsCount,

        @JsonIgnore
        Integer maxMeaningContinuousSequence

) implements Comparable<SearchResult> {

    @Override
    public int compareTo(SearchResult o) {

        return o.maxMeaningContinuousSequence.compareTo(maxMeaningContinuousSequence) * 1000 +
                o.meaningsCount.compareTo(meaningsCount) * 100 +
                o.relevance.compareTo(relevance);
    }
}
