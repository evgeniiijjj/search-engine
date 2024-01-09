package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Page;
import searchengine.entities.Site;

import java.util.Optional;


@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Modifying
    @Query(value = "INSERT INTO pages (site_id, page_path, code, page_content) " +
            "VALUES (:#{#page.siteId}, :#{#page.path}, :#{#page.code}, :#{#page.content}) " +
            "ON DUPLICATE KEY UPDATE code=:#{#page.code}, page_content=:#{#page.content}",
            nativeQuery = true)
    void insertOrUpdate(Page page);

    Optional<Page> findBySiteAndPath(Site site, String path);

    int countBySite(Site site);
}
