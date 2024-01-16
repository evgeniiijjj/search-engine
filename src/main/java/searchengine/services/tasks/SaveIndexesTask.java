package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import searchengine.entities.Index;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Constants;
import searchengine.enums.Statuses;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@AllArgsConstructor
public class SaveIndexesTask implements IndexingTask {

    private final IndexingManager indexingManager;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private Set<Site> sites;


    @Override
    public void run() {

        try {
            Map<Page, Set<Index>> indexes = indexingManager.getIndexes();
            int count = 0;
            while (count < Constants.WAITING_CYCLES_NUM.getValue()) {
                Thread.sleep(Constants.TIMEOUT_150_MS.getValue());
                if (!indexes.isEmpty()) {
                    count = 0;
                    saveIndexes(indexes);
                }
                count++;
            }
            updateSiteStatus();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void saveIndexes(Map<Page, Set<Index>> indexes) {

        Map<Page, Set<Index>> indexesButch = new HashMap<>(indexes);
        indexesButch.keySet().forEach(indexes::remove);
        indexesButch.keySet().forEach(this::removeOldPageIndexes);
        indexesButch.keySet().stream().map(Page::getSite).forEach(sites::add);
        indexRepository.saveAll(
                indexesButch.values().stream().flatMap(Set::stream).toList()
        );
        indexesButch.forEach((page, indexSet) ->
            indexSet.forEach(index -> {
                    if (lemmaRepository.existsSiteLemma(page.getSite(), index.getLemma()) == 0) {
                        lemmaRepository.insertSiteLemma(page.getSite(), index.getLemma());
                    }
            })
        );
    }

    private void removeOldPageIndexes(Page page) {

        indexRepository.deleteAllSiteLemmasByPage(page);
        indexRepository.deleteAllByPage(page);
    }

    private void updateSiteStatus() {

        sites.forEach(site -> {
                    site.setStatus(Statuses.INDEXED);
                    site.setStatusTime(Instant.now().truncatedTo(ChronoUnit.SECONDS));
                    site.setLastError(null);
                    siteRepository.save(site);
        });
    }
}
