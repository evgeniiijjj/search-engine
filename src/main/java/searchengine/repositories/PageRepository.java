package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.entities.Page;
import searchengine.entities.Site;

import java.util.Optional;


public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findBySiteAndPath(Site site, String path);

    boolean existsBySiteAndPath(Site site, String path);

    int countBySite(Site site);
}
