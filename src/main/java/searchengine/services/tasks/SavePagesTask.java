package searchengine.services.tasks;

import lombok.AllArgsConstructor;
import searchengine.entities.Page;
import searchengine.enums.Constants;
import searchengine.enums.Statuses;
import searchengine.services.IndexingManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@AllArgsConstructor
public class SavePagesTask implements Runnable {

    private final IndexingManager indexingManager;

    @Override
    public void run() {
        while (indexingManager.isActiveThreadCountMoreThanOne()) {
            try {
                Thread.sleep(Constants.TIMEOUT_150_MS.getValue());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            savePages();
        }
        updateSiteStatus();
    }

    private void savePages() {
        List<Page> savedPages = indexingManager
                .getPageRepository()
                .saveAllAndFlush(
                        indexingManager.getPages()
                                .stream()
                                .filter(page -> page.getId() == null)
                                .peek(page -> page.setId(0))
                                .toList()
                );
        savedPages.forEach(
                indexingManager::startIndexingPagesTask
        );
    }

    private void updateSiteStatus() {
        indexingManager.getSites().forEach(site -> {
            site.setStatus(Statuses.INDEXED);
            site.setStatusTime(
                    Instant.now()
                            .truncatedTo(ChronoUnit.SECONDS));
            site.setLastError("");
            indexingManager.getSiteRepository().save(site);
        });
    }
}
