package searchengine.models.statistics;

import java.util.List;

public record StatisticsData(
        TotalStatistics total,
        List<DetailedStatisticsItem> details
) {
}
