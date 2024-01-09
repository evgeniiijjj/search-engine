package searchengine.services;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import searchengine.entities.Page;
import searchengine.services.tasks.FindPagesTask;
import searchengine.services.tasks.IndexingPageTask;
import searchengine.services.tasks.IndexingTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
@EnableAsync
public class IndexingManager {

    private final ThreadPoolTaskExecutor executor;
    private final ApplicationContext context;

    private final Set<Page> indexedPages;

    public IndexingManager(ThreadPoolTaskExecutor executor,
                           ApplicationContext applicationContext) {

        this.executor = executor;
        this.context = applicationContext;
        this.indexedPages = ConcurrentHashMap.newKeySet();
    }

    private IndexingTask getTask(String name) {

        name = name.substring(0, 1).toLowerCase()
                .concat(name.substring(1));
        return (IndexingTask) context
                .getBean(name);
    }

    @Async
    public void startFindPageWithoutSubpagesTask(Page page) {

        getTask(FindPagesTask.class.getSimpleName())
                .setPage(page)
                .withoutFindSubpages()
                .run();
    }

    @Async
    public void startFindPageTask(Page page) {

        getTask(FindPagesTask.class.getSimpleName())
                .setPage(page)
                .run();
    }

    @Async
    public void startIndexingPageTask(Page page) {

        getTask(IndexingPageTask.class.getSimpleName())
                .setPage(page)
                .run();
    }

    public boolean isIndexing() {

        return executor.getActiveCount() != 0;
    }

    public void stopIndexing() {

        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.shutdown();
        executor.initialize();
    }

    public boolean isPageIndexed(Page page) {

        return indexedPages.contains(page);
    }

    public void addIndexedPage(Page page) {

        indexedPages.add(page);
    }
}
