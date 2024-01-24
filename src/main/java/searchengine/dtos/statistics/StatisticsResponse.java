package searchengine.dtos.statistics;

public record StatisticsResponse(
        boolean result,
        StatisticsData statistics
) {
}
