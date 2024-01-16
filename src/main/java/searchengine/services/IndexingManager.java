package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import searchengine.entities.Index;
import searchengine.entities.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.tasks.IndexingPageTask;
import searchengine.services.tasks.IndexingTask;
import searchengine.services.tasks.SaveIndexesTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
@AllArgsConstructor
@Getter
public class IndexingManager {

    private final ThreadPoolTaskExecutor executor;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private Map<Page, Set<Index>> indexes = new ConcurrentHashMap<>();

    private IndexingTask getIndexingPageTask() {

        return new IndexingPageTask(
                this,
                siteRepository,
                pageRepository,
                lemmaRepository,
                null
        );
    }

    private IndexingTask getSaveIndexesTask() {

        return new SaveIndexesTask(
                this,
                siteRepository,
                lemmaRepository,
                indexRepository,
                new HashSet<>()
        );
    }

    public void startIndexingPageTask(Page page) {

        executor.submit(getIndexingPageTask()
                .setPage(page));
    }

    public void startSaveIndexesTask() {

        executor.submit(getSaveIndexesTask());
    }

    public boolean isIndexing() {

        return executor.getActiveCount() > 0;
    }

    public void stopIndexing() {

        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.shutdown();
        executor.initialize();
        indexes = new ConcurrentHashMap<>();
    }

    public void putIndexes(Page page, Set<Index> indexes) {

        this.indexes.put(page, indexes);
    }
}
