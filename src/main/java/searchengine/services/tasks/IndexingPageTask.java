package searchengine.services.tasks;

import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.jsoup.Jsoup;
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
    private Page page;

    @Override
    public IndexingPageTask setPage(Page page) {

        this.page = page;
        deleteOldIndexes();
        return this;
    }

    @Override
    public void run() {

        assert page != null;
        String text = Jsoup.parse(page.getContent()).body().text();
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
                            .distinct()
                            .map(this::saveLemma)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors
                                    .toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue
                                    )
                            );

            saveIndexes(lemmas);

            updateSiteStatus(Statuses.INDEXED, null);

        }catch (Exception e) {

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

    private Optional<Map.Entry<Lemma, Integer>> saveLemma(Map.Entry<String, Integer> lemmaEntry) {

        Lemma lemma = new Lemma(lemmaEntry.getKey());

        lemmaRepository.insertLemma(lemma);

        return lemmaRepository.findByLemma(lemma.getLemma()).map(
                l -> {
                    assert page != null;
                    if (lemmaRepository.countLemmaSiteRowsByLemmaAndSite(l, page.getSite()) == 0) {

                        lemmaRepository.insertLemmaSite(l, page.getSite());
                    }
                    return new DefaultMapEntry<>(l, lemmaEntry.getValue());
                }
        );
    }

    private void saveIndexes(Map<Lemma, Integer> lemmas) {

        assert page != null;
        lemmas
                .entrySet()
                .stream()
                .map(entry ->
                        new Index(
                                page,
                                entry.getKey(),
                                (float) entry.getValue() / lemmas.size()
                            )
                )
                .forEach(indexRepository::insertOrUpdate);
    }

    private void deleteOldIndexes() {

        indexRepository.deleteAllSiteLemmasByPage(page);
        indexRepository.deleteAllByPage(page);
    }
}
