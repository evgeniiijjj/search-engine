package searchengine.services.utils;

import lombok.Getter;
import searchengine.enums.Patterns;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Getter
public class NodeTextProcessor {

    private final String text;
    private final Set<WordMeaningPosition> wordMeaningPositions;


    public NodeTextProcessor(String text) {

        this.text = text;
        wordMeaningPositions = new HashSet<>();
    }

    public boolean isWordMeaningPositionsNotEmpty() {

        return !wordMeaningPositions.isEmpty();
    }

    public int getWordMeaningCount() {

        return wordMeaningPositions.size();
    }

    public void addPosition(int start, int end) {

        wordMeaningPositions.add(
                new WordMeaningPosition(start, end)
        );
    }

    public boolean containsMeaning(String meaning) {

        return text.contains(meaning);
    }

    public String getSnippetPart() {

        Set<Integer> meaningPositions =
                wordMeaningPositions
                        .stream()
                        .flatMap(wmp ->
                                Stream.of(
                                        wmp.start,
                                        wmp.end
                                )
                        )
                        .collect(Collectors.toSet());

        meaningPositions.add(text.length());

        AtomicInteger prevPos = new AtomicInteger();
        AtomicInteger countPos = new AtomicInteger();

        return meaningPositions
                .stream()
                .sorted()
                .map(pos ->
                        modifyString(
                                countPos.getAndIncrement(),
                                prevPos.getAndSet(pos),
                                pos
                        )
                )
                .reduce(
                        new StringBuilder(),
                        this::buildString,
                        StringBuilder::append
                )
                .toString();
    }

    private String modifyString(int countPos, int prevPos, int currentPos) {

        String str = text.substring(prevPos, currentPos);

        if (countPos % 2 > 0) {

            return Patterns.HIGHLIGHTED_STRING_PART
                    .getStringValue(str);
        }

        if (prevPos == 0) {

            return Patterns.FIRST_STRING_PART.modifyString(str);
        } else if (currentPos == text.length()) {

            return Patterns.LAST_STRING_PART.modifyString(str);
        } else {

            return Patterns.MIDDLE_STRING_PART.modifyString(str);
        }
    }

    public StringBuilder buildString(StringBuilder sb, String str) {

        return sb.append(str);
    }

    private static class WordMeaningPosition {

        private final int start;
        private int end;

        private WordMeaningPosition(int start, int end) {

            this.start = start;
            this.end = end;
        }

        @Override
        public int hashCode() {

            return start;
        }

        @Override
        public boolean equals(Object o) {

            if (!o.getClass().equals(WordMeaningPosition.class)) {

                return false;
            }
            WordMeaningPosition wmp = (WordMeaningPosition) o;

            if (wmp.end > end) {

                end = wmp.end;
            } else {

                wmp.end = end;
            }

            return true;
        }
    }
}
