package searchengine.models.statistics;

public record DetailedStatisticsItem(
        String url,
        String name,
        String status,
        long statusTime,
        String error,
        int pages,
        int lemmas
) {
}
