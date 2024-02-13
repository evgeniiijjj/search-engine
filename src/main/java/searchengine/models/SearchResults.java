package searchengine.models;

import lombok.Getter;

import java.util.List;

@Getter
public class SearchResults {
    private final boolean result;
    private final int count;
    private final List<SearchResult> data;

    public SearchResults(List<SearchResult> data) {
        result = true;
        count = data.size();
        this.data = data;
    }

    public SearchResults(List<SearchResult> data, int count) {
        result = true;
        this.count = count;
        this.data = data;
    }

    public SearchResults getPagedResults(int offset, int limit) {
        return new SearchResults(
                data.stream().skip(offset).limit(limit).toList(),
                count
        );
    }
}