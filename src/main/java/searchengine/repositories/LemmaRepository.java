package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Lemma;
import searchengine.entities.Site;

import java.util.Optional;


public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query(value = "SELECT EXISTS(SELECT * FROM site_lemmas sl " +
            "WHERE sl.site_id=:#{#site.id} AND sl.lemma_id=:#{#lemma.id})",
            nativeQuery = true)
    long existsSiteLemma(Site site, Lemma lemma);

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO site_lemmas(site_id, lemma_id) " +
            "VALUE(:#{#site.id}, :#{#lemma.id})",
            nativeQuery = true)
    void insertSiteLemma(Site site, Lemma lemma);

    @Query(value = "SELECT DISTINCT COUNT(sl.lemma_id) FROM site_lemmas sl",
            nativeQuery = true)
    long siteLemmaCount();

    Optional<Lemma> findByLemma(String lemma);
}
