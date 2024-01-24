package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Page;
import searchengine.entities.Site;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findBySiteAndPath(Site site, String path);

    boolean existsBySiteAndPath(Site site, String path);

    int countBySite(Site site);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM pages p WHERE p.site_id=:#{#site.id}",
            nativeQuery = true)
    void deleteAllBySite(Site site);
}
