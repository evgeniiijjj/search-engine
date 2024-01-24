package searchengine.dtos.statistics;

public record TotalStatistics(
        int sites,
        int pages,
        int lemmas,
        boolean indexing
) {
}
