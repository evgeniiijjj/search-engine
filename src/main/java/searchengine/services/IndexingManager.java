package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import searchengine.entities.Page;
import searchengine.entities.Site;
import searchengine.enums.Patterns;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.tasks.IndexingPagesTask;
import searchengine.services.tasks.SavePagesTask;
import searchengine.services.tasks.SearchPagesTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
@Getter
public class IndexingManager {

    private final ThreadPoolTaskExecutor executor;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    private final Set<Site> sites = ConcurrentHashMap.newKeySet();
    private final Set<Page> pages = ConcurrentHashMap.newKeySet();

    public void startIndexingPagesTask(Page page) {
        executor.submit(new IndexingPagesTask(this, page));
    }

    public void startSavePagesTask() {
        executor.submit(new SavePagesTask(this));
    }

    public void startSearchPagesTask(Page page) {
        executor.submit(new SearchPagesTask(this, page));
    }

    public void startIndexing() {
        sites.forEach(site ->
                startSearchPagesTask(
                        new Page(site, Patterns.ROOT_PATH.getStringValue())
                )
        );
        startSavePagesTask();
    }

    public boolean isIndexing() {
        return executor.getActiveCount() > 0;
    }

    public boolean isActiveThreadCountMoreThanOne() {
        return executor.getActiveCount() > 1;
    }

    public void stopIndexing() {
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.shutdown();
        executor.initialize();
    }

    public void setSites(Set<Site> sites) {
        this.sites.addAll(sites);
    }

    public void addPage(Page page) {
        pages.add(page);
    }

    public boolean isPagePresent(Page page) {
        return pages.contains(page);
    }
}
