package searchengine.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Snippet implements Comparable<Snippet> {
    private String snippet;
    private final List<MeaningPositions> positions;
    private final Integer wordMeaningsVarietyCount;

    public int getWordMeaningCount() {
        return positions.size();
    }

    public boolean isNotBlank() {
        return !snippet.isBlank();
    }

    @Override
    public String toString() {
        return snippet;
    }

    @Override
    public int compareTo(Snippet o) {
        return wordMeaningsVarietyCount
                .compareTo(o.wordMeaningsVarietyCount) * 1000 +
                Integer.compare(positions.size(), o.positions.size());
    }
}
