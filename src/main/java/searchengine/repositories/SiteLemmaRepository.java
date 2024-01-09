package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.entities.Lemma;
import searchengine.entities.Site;
import searchengine.entities.SiteLemma;

import java.util.List;
import java.util.Optional;


@Transactional
public interface SiteLemmaRepository extends JpaRepository<SiteLemma, Integer> {

    @Modifying
    @Query(value = "INSERT INTO site_lemmas (site_id, lemma_id, frequency) " +
            "VALUES (:#{#siteLemma.siteId}, :#{#siteLemma.lemmaId}, 1) " +
            "ON DUPLICATE KEY UPDATE frequency=frequency+1",
            nativeQuery = true)
    void insertOrUpdate(SiteLemma siteLemma);

    @Modifying
    @Query(value = "UPDATE site_lemmas SET frequency=frequency-1 WHERE id=:#{#siteLemma.id}",
            nativeQuery = true)
    void updateSiteLemmaFrequency(SiteLemma siteLemma);

    Optional<SiteLemma> findBySiteAndLemma(Site site, Lemma lemma);

    int countBySite(Site site);
}
