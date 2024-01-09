package searchengine.services.tasks;

import searchengine.entities.Page;


public interface IndexingTask {

    IndexingTask setPage(Page page);

    void run();

    default IndexingTask withoutFindSubpages() {

        return this;
    }
}
