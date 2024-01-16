package searchengine.services;

import searchengine.dto.SearchResult;
import searchengine.dto.statistics.StatisticsResponse;

import java.util.List;


public interface IndexingService {
    boolean startIndexing();
    boolean stopIndexing();
    boolean indexPage(String url);
    StatisticsResponse getStatistics();
    List<SearchResult> getSearchResults(String query, String site, int offset, int limit);
}
