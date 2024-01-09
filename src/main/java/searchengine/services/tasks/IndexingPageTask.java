package searchengine.services.tasks;

import com.github.demidko.aot.WordformMeaning;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.entities.*;
import searchengine.enums.Statuses;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteLemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.utils.LemmaProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@AllArgsConstructor
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IndexingPageTask implements IndexingTask {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteLemmaRepository siteLemmaRepository;
    private final IndexRepository indexRepository;

    @Nullable
    private Page page;

    @Override
    public IndexingPageTask setPage(Page page) {

        this.page = page;
        return this;
    }

    @Override
    public void run() {

        assert page != null;
        String text = page.getContent();
        updateSiteStatus(Statuses.INDEXING, null);

        try {

            Set<Map.Entry<Lemma, Integer>> lemmas = LemmaProcessor
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
                    .collect(Collectors.toSet());

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
        }

        saveSiteLemma(lemma);

        return new DefaultMapEntry<>(lemma, lemmaEntry.getValue());
    }

    private void saveSiteLemma(Lemma lemma) {

        assert page != null;
        SiteLemma siteLemma =
                new SiteLemma(page.getSite(), lemma);

        siteLemmaRepository.insertOrUpdate(siteLemma);
    }

    private void saveIndexes(Set<Map.Entry<Lemma, Integer>> entries) {

        assert page != null;
        entries
                .stream()
                .map(entry ->
                        new Index(
                                page,
                                entry.getKey(),
                                (float) entry.getValue() / entries.size()
                            )
                )
                .forEach(indexRepository::insertIndex);
    }

    private void deleteOldPageIndexes(Page page) {

        List<Index> pageIndexes = indexRepository
                .findAllByPageId(page.getId());

        List<SiteLemma> siteLemmas = pageIndexes
                .stream()
                .map(Index::getLemma)
                .map(lemma -> siteLemmaRepository
                        .findBySiteAndLemma(page.getSite(), lemma))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        siteLemmas
                .stream()
                .filter(siteLemma -> siteLemma.getFrequency() < 2)
                .forEach(siteLemmaRepository::delete);

        siteLemmas
                .stream()
                .filter(siteLemma -> siteLemma.getFrequency() > 1)
                .forEach(siteLemmaRepository::updateSiteLemmaFrequency);

        indexRepository.deleteAll(pageIndexes);
    }
}
