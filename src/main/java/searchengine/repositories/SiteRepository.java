package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.entities.Site;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    Optional<Site> findByUrl(String url);
}
