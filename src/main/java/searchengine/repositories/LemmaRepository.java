package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Lemma;
import searchengine.entities.Site;

import java.util.Optional;


public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO lemmas (lemma) VALUES (:#{#lemma.lemma})", nativeQuery = true)
    void insertLemma(Lemma lemma);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM site_lemmas sl WHERE sl.lemma_id=:#{#lemma.id})", nativeQuery = true)
    void deleteLemmaSiteByLemma(Lemma lemma);

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO site_lemmas (site_id, lemma_id)" +
            " VALUES (:#{#site.id}, :#{#lemma.id})", nativeQuery = true)
    void insertLemmaSite(Lemma lemma, Site site);

    Optional<Lemma> findByLemma(String lemma);
}
