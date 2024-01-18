package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.entities.Site;

import java.util.Optional;


public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query(value = "SELECT count(*) FROM sites s RIGHT JOIN site_lemmas sl " +
            "ON s.id=sl.site_id WHERE s.id=:#{#site.id}",
            nativeQuery = true)
    int lemmasCount(Site site);

    Optional<Site> findByUrl(String url);
}
