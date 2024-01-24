package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Lemma;
import searchengine.entities.Site;
import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemmas (site_id, lemma, frequency) " +
            "VALUE(:#{#lemma.siteId}, :#{#lemma.lemma}, 1) " +
            "ON DUPLICATE KEY UPDATE frequency=frequency+1",
            nativeQuery = true)
    void insertOrUpdateLemma(Lemma lemma);

    Optional<Lemma> findBySiteAndLemma(Site site, String lemma);

    List<Lemma> findByLemma(String lemma);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM lemmas l WHERE l.site_id=:#{#site.id}",
            nativeQuery = true)
    void deleteAllBySite(Site site);
}
