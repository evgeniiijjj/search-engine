package searchengine.services.tasks;

import searchengine.entities.Page;

public interface IndexingTask extends Runnable {
    IndexingTask setPage(Page page);
}
