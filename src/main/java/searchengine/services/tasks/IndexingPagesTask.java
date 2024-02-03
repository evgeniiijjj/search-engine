package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.entities.Index;
import searchengine.entities.Page;
import searchengine.enums.Patterns;
import searchengine.services.IndexingManager;
import searchengine.services.utils.LemmaProcessor;
import searchengine.models.WordFormMeanings;
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
        Document document = Jsoup.parse(page.getContent());
        String text = document.body()
                .getAllElements()
                .stream()
                .map(this::getTextFromElement)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(
                        new StringBuilder(),
                        (sb, str) -> sb.append(" ").append(str),
                        StringBuilder::append
                )
                .toString();
        Map<String, Integer> lemmas =
                LemmaProcessor
                        .getLemmas(text)
                        .stream()
                        .map(WordFormMeanings::toString)
                        .collect(
                                Collectors.toMap(
                                        lemma -> lemma,
                                        lemma -> 1,
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
