package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Page;
import searchengine.entities.Site;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findBySiteAndPath(Site site, String path);

    long countBySite(Site site);

    @Transactional
    void deleteAllBySite(Site site);
}
