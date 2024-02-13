package searchengine.models;

public record MeaningPositions(
        int start,
        int end
) implements Comparable<MeaningPositions> {
    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(MeaningPositions.class)) {
            return false;
        }
        MeaningPositions mp = (MeaningPositions) o;
        if (start >= mp.start) {
            return end <= mp.end;
        }
        return false;
    }

    @Override
    public int compareTo(MeaningPositions o) {
        return (start - o.start) * 1000 + o.end - end;
    }
}
