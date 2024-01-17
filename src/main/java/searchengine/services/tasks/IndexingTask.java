package searchengine.services.tasks;
import searchengine.entities.Page;


public interface IndexingTask extends Runnable {

    void run();
    IndexingTask setPage(Page page);
}
