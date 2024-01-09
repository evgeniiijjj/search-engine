package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Site;

import java.util.Optional;


public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO sites (site_status, status_time, last_error, site_url, site_name) " +
            "VALUES (:#{#site.status}, :#{#site.statusTime}, :#{#site.lastError}, :#{#site.url}, :#{#site.name}) " +
            "ON DUPLICATE KEY UPDATE site_status=:#{#site.status}, status_time=:#{#site.statusTime}, " +
            "last_error=:#{#site.lastError}, site_url=:#{#site.url}, site_name=:#{#site.name}", nativeQuery = true)
    void insertOrUpdate(Site site);

    Optional<Site> findByUrl(String url);
}
