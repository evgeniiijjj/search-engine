package searchengine.services.tasks;

import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Statuses;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.utils.LemmaProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@AllArgsConstructor
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IndexingPageTask implements IndexingTask {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Nullable
    private Set<Lemma> oldPageLemmas;
    @Nullable
    private Page page;

    @Override
    public IndexingPageTask setPage(Page page) {

        this.page = page;
        fillOldPageLemmas();
        return this;
    }

    @Override
    public void run() {

        assert page != null;
        String text = page.getContent();
        updateSiteStatus(Statuses.INDEXING, null);

        try {

            Map<Lemma, Integer> lemmas =
                    LemmaProcessor
                            .getRussianLemmas(text)
                            .stream()
                            .map(WordformMeaning::toString)
                            .collect(
                                    Collectors.toMap(
                                            lemma -> lemma,
                                            lemma -> 1,
                                            Integer::sum
                                    )
                            )
                            .entrySet()
                            .stream()
                            .map(this::saveLemma)
                            .collect(Collectors
                                    .toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue
                                    )
                            );

            deleteOldPageLemmas();

            deleteOldPageIndexes(page);

            saveIndexes(lemmas);

            updateSiteStatus(Statuses.INDEXED, null);

        } catch (Exception e) {

            updateSiteStatus(Statuses.FAILED, e.getMessage());

            e.printStackTrace();
        }
    }

    private void updateSiteStatus(Statuses status, String lastError) {

        assert page != null;
        Site site = page.getSite();
        site.setStatus(status);
        site.setStatusTime(
                Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS)
        );
        site.setLastError(lastError);
        siteRepository.save(site);
    }

    private Map.Entry<Lemma, Integer> saveLemma(Map.Entry<String, Integer> lemmaEntry) {

        Lemma lemma = new Lemma(lemmaEntry.getKey());

        lemmaRepository.insertLemma(lemma);

        Optional<Lemma> optional = lemmaRepository
                    .findByLemma(lemma.getLemma());

        if (optional.isPresent()) {

            lemma = optional.get();

            assert oldPageLemmas != null;
            oldPageLemmas.remove(lemma);

            assert page != null;
            lemmaRepository.insertLemmaSite(lemma, page.getSite());
        }

        return new DefaultMapEntry<>(lemma, lemmaEntry.getValue());
    }

    private void saveIndexes(Map<Lemma, Integer> lemmas) {

        assert page != null;
        List<Index> indexes = lemmas
                .entrySet()
                .stream()
                .map(entry ->
                        new Index(
                                page,
                                entry.getKey(),
                                (float) entry.getValue() / lemmas.size()
                            )
                )
                .toList();

        indexRepository.saveAllAndFlush(indexes);
    }

    private void deleteOldPageLemmas() {

        assert oldPageLemmas != null;
        oldPageLemmas.forEach(
            lemmaRepository::deleteLemmaSiteByLemma
        );
    }

    private void deleteOldPageIndexes(Page page) {

        indexRepository.deleteAllByPage(page);
    }

    private void fillOldPageLemmas() {

        oldPageLemmas = new HashSet<>();
        indexRepository
                .findAllByPage(page)
                .stream()
                .map(Index::getLemma)
                .forEach(oldPageLemmas::add);
    }
}
