package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Lemma;

import java.util.Optional;


public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO lemmas (lemma) VALUES (:#{#lemma.lemma})", nativeQuery = true)
    void insertLemma(Lemma lemma);

    Optional<Lemma> findByLemma(String lemma);
}
