package searchengine.services;

import searchengine.dto.SearchResult;
import searchengine.dto.statistics.StatisticsResponse;

import java.util.List;
import java.util.Optional;


public interface Service {
    boolean startIndexing();
    boolean stopIndexing();
    boolean indexPage(String url);

    StatisticsResponse getStatistics();
    Optional<List<SearchResult>> getSearchResults(String query, String site, int offset, int limit);
}
