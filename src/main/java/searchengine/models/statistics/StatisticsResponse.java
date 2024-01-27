package searchengine.models.statistics;

public record StatisticsResponse(
        boolean result,
        StatisticsData statistics
) {
}
