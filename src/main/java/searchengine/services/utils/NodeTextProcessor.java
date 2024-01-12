package searchengine.services.utils;

import com.github.demidko.aot.WordformMeaning;
import lombok.Getter;
import searchengine.dto.PageLemmas;
import searchengine.dto.Snippet;
import searchengine.enums.Patterns;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Getter
public class NodeTextProcessor {

    private final String text;
    private final Set<WordMeaningPosition> wordMeaningPositions;
    private int maxMeaningsContinuousSequence;
    private int counter;

    public NodeTextProcessor(String text, PageLemmas pageLemmas) {

        this.text = text;
        wordMeaningPositions = new HashSet<>();
        pageLemmas
                .lemmas()
                .stream()
                .map(WordformMeaning::getTransformations)
                .flatMap(List::stream)
                .map(WordformMeaning::toString)
                .filter(this::containsMeaning)
                .distinct()
                .forEach(meaning -> Patterns.SAMPLE
                        .getPattern(meaning)
                        .matcher(getText().toLowerCase())
                        .results()
                        .forEach(result ->
                                addPosition(
                                        result.start(),
                                        result.end()
                                )
                        )
                );
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

        return text.toLowerCase().contains(meaning);
    }

    public Snippet getSnippet() {

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

        String stringSnippet = meaningPositions
                .stream()
                .sorted()
                .map(pos ->
                        modifyString(
                                countPos.getAndIncrement(),
                                prevPos.getAndSet(pos),
                                pos
                        )
                )
                .filter(str -> !str.isEmpty())
                .reduce(
                        new StringBuilder(),
                        StringBuilder::append,
                        StringBuilder::append
                )
                .toString();
        return new Snippet(
                stringSnippet,
                getWordMeaningCount(),
                maxMeaningsContinuousSequence
        );
    }

    private String modifyString(int countPos, int prevPos, int currentPos) {

        String str = text.substring(prevPos, currentPos);

        if (countPos % 2 > 0) {

            counter++;

            return Patterns.HIGHLIGHTED_STRING_PART
                    .getStringValue(str);
        }

        if (!str.equals(Patterns.ONE_SPACE.getStringValue())) {

            if (counter > maxMeaningsContinuousSequence) {

                maxMeaningsContinuousSequence = counter;
            }
            counter = 0;
        }

        if (prevPos == 0) {

            return Patterns.FIRST_STRING_PART.modifyString(str);
        } else if (currentPos == text.length()) {

            return Patterns.LAST_STRING_PART.modifyString(str);
        } else {

            return Patterns.MIDDLE_STRING_PART.modifyString(str);
        }
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

            if (start != wmp.start) {

                return false;
            }

            if (wmp.end < end) {

                wmp.end = end;
            }

            return true;
        }
    }
}
