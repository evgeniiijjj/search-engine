package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Page;
import searchengine.entities.Site;

import java.util.Optional;


public interface PageRepository extends JpaRepository<Page, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO pages(site_id, page_path, code, page_content) " +
            "VALUE(:#{#page.siteId}, :#{#page.path}, :#{#page.code}, :#{#page.content})",
            nativeQuery = true)
    void insertPage(Page page);

    Optional<Page> findBySiteAndPath(Site site, String path);

    boolean existsBySiteAndPath(Site site, String path);

    int countBySite(Site site);
}
