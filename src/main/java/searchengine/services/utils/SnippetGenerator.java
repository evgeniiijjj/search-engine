package searchengine.services.utils;

import com.github.demidko.aot.WordformMeaning;
import lombok.Getter;
import searchengine.dto.PageLemmas;
import searchengine.dto.Snippet;
import searchengine.enums.Patterns;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Getter
public class SnippetGenerator {

    private final String text;
    private final Map<Integer, Integer> meaningPositions;
    private int maxMeaningsContinuousSequence;
    private int counter;

    public SnippetGenerator(String text, PageLemmas pageLemmas) {
        this.text = text;
        meaningPositions = new HashMap<>();
        fillWordMeaningPositions(pageLemmas);
    }

    private void fillWordMeaningPositions(PageLemmas pageLemmas) {
        pageLemmas
                .lemmas()
                .stream()
                .map(WordformMeaning::getTransformations)
                .flatMap(List::stream)
                .map(WordformMeaning::toString)
                .filter(this::containsMeaning)
                .distinct()
                .forEach(meaning -> Patterns.SAMPLE
                        .getRedexPattern(meaning)
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

    public void addPosition(int start, int end) {
        if (!meaningPositions.containsKey(start) ||
                meaningPositions.get(start) < end) {
            meaningPositions.put(start, end);
        }
    }

    public boolean containsMeaning(String meaning) {
        return text.toLowerCase().contains(meaning);
    }

    public Snippet getSnippet() {
        Set<Integer> positions =
                meaningPositions
                        .entrySet()
                        .stream()
                        .flatMap(entry ->
                                Stream.of(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .collect(Collectors.toSet());
        positions.add(text.length());
        AtomicInteger prevPos = new AtomicInteger();
        AtomicInteger countPos = new AtomicInteger();
        String stringSnippet = positions
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
                getMeaningPositionsCount(),
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
            return Patterns.FIRST_STRING_PART
                    .getStringValue(str);
        } else if (currentPos == text.length()) {
            return Patterns.LAST_STRING_PART
                    .getStringValue(str);
        } else {
            return Patterns.MIDDLE_STRING_PART
                    .getStringValue(str);
        }
    }

    public boolean isMeaningPositionsNotEmpty() {
        return !meaningPositions.isEmpty();
    }

    public int getMeaningPositionsCount() {
        return meaningPositions.size();
    }
}