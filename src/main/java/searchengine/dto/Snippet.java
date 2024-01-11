package searchengine.dto;

import searchengine.enums.Patterns;


public record Snippet(
        String stringSnippet,
        int wordMeaningCount,
        int maxMeaningContinuousSequence
) {
    public Snippet accumulate(Snippet snippet) {

        return new Snippet(
               accumulate(snippet.stringSnippet),
               wordMeaningCount + snippet.wordMeaningCount,
                Integer.max(maxMeaningContinuousSequence,
                        snippet.maxMeaningContinuousSequence)
        );
    }

    private String accumulate(String stringSnippet) {

        if (this.stringSnippet.length() >
                Patterns.MAX_SNIPPET_LENGTH.getIntValue()) {

            return this.stringSnippet;
        }
        return Patterns.REMOVE_REDUNDANT_SYMBOLS
                .modifyString(
                        this.stringSnippet
                                .concat(Patterns.END_LINE.getStringValue())
                                .concat(stringSnippet)
                );
    }

    @Override
    public String toString() {

        return stringSnippet;
    }
}
