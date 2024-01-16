package searchengine.services.tasks;
import searchengine.entities.Page;


public interface IndexingTask extends Runnable {

    void run();
    default IndexingTask setPage(Page page) {
        return this;
    }
}
