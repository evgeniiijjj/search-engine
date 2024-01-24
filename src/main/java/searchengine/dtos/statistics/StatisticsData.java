package searchengine.dtos.statistics;

import java.util.List;

public record StatisticsData(
        TotalStatistics total,
        List<DetailedStatisticsItem> details
) {
}
