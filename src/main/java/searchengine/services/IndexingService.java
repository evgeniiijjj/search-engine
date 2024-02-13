package searchengine.services;

import searchengine.models.SearchResults;
import searchengine.models.statistics.StatisticsResponse;

public interface IndexingService {
    boolean startIndexing();
    boolean stopIndexing();
    boolean indexPage(String url);
    StatisticsResponse getStatistics();
    SearchResults getSearchResults(String query, String site, int offset, int limit);
}
