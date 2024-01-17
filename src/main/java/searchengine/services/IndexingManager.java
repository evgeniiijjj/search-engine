package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import searchengine.entities.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.tasks.IndexingPageTask;
import searchengine.services.tasks.IndexingTask;


@Component
@AllArgsConstructor
@Getter
public class IndexingManager {

    private final ThreadPoolTaskExecutor executor;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private IndexingTask getIndexingPageTask() {

        return new IndexingPageTask(
                this,
                siteRepository,
                pageRepository,
                lemmaRepository,
                indexRepository,
                null
        );
    }

    public void startIndexingPageTask(Page page) {

        executor.submit(getIndexingPageTask()
                .setPage(page));
    }

    public boolean isIndexing() {

        return executor.getActiveCount() > 0;
    }

    public void stopIndexing() {

        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.shutdown();
        executor.initialize();
    }
}
