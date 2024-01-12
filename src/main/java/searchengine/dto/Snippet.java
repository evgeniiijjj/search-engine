package searchengine.dto;


public record Snippet(
        String stringSnippet,
        Integer wordMeaningCount,
        Integer maxMeaningContinuousSequence
) implements Comparable<Snippet> {

    @Override
    public String toString() {

        return stringSnippet;
    }

    @Override
    public int compareTo(Snippet o) {

        return maxMeaningContinuousSequence
                .compareTo(o.maxMeaningContinuousSequence) * 1000 +
                wordMeaningCount.compareTo(o.wordMeaningCount);
    }
}
