package searchengine.services;

import searchengine.dtos.SearchResult;
import searchengine.dtos.statistics.StatisticsResponse;
import java.util.List;

public interface IndexingService {
    boolean startIndexing();
    boolean stopIndexing();
    boolean indexPage(String url);
    StatisticsResponse getStatistics();
    List<SearchResult> getSearchResults(String query, String site, int offset, int limit);
}
