package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import searchengine.entities.Index;
import searchengine.entities.Page;
import searchengine.enums.Patterns;
import searchengine.models.Meaning;
import searchengine.services.IndexingManager;
import searchengine.services.utils.LemmaProcessor;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class IndexingPagesTask implements Runnable {

    private final IndexingManager indexingManager;
    private Page page;

    @Override
    public void run() {
        Map<String, Integer> lemmas = Jsoup.parse(page.getContent())
                .body().getAllElements()
                .stream()
                .map(this::getTextFromElement)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(text -> LemmaProcessor
                        .getLemmas(text, false)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Meaning::word,
                                        lemma -> 1,
                                        Integer::sum
                                )
                        )
                        .entrySet()
                        .stream()
                )
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Integer::sum
                        )
                );
        saveIndexes(lemmas);
    }

    private Optional<String> getTextFromElement(Element element) {
        if (Patterns.HTML_TEXT_TAG_NAMES
                .isMatches(element.nodeName())) {
            return Optional.of(
                    element.text()
            );
        }
        return Optional.empty();
    }

    private void saveIndexes(Map<String, Integer> lemmas) {
        int lemmasNum = lemmas.values().stream()
                .reduce(Integer::sum)
                .orElse(1);
        indexingManager
                .getIndexRepository()
                .saveAllAndFlush(
                        lemmas
                                .entrySet()
                                .stream()
                                .map(entry ->
                                        new Index(
                                                page,
                                                entry.getKey(),
                                                (float) entry.getValue() / lemmasNum
                                        )
                                )
                                .toList()
        );
    }
}
