package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;

import java.util.List;


public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO indexes (page_id, lemma_id, lemma_rank) " +
            "VALUES (:#{#index.pageId}, :#{#index.lemmaId}, :#{#index.rank}) " +
            "ON DUPLICATE KEY UPDATE lemma_rank=:#{#index.rank}",
            nativeQuery = true)
    void insertOrUpdate(Index index);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM site_lemmas sl WHERE sl.lemma_id IN " +
            "(SELECT ind.lemma_id FROM indexes ind WHERE ind.page_id=:#{#page.id})",
            nativeQuery = true)
    void deleteAllSiteLemmasByPage(Page page);

    @Query(value = "SELECT * FROM indexes WHERE lemma_id=:#{#lemma.id} " +
            "ORDER BY lemma_rank DESC LIMIT :#{#limit}",
            nativeQuery = true)
    List<Index> findAllByLemmaOrderByRankDescLimit(Lemma lemma, int limit);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM indexes ind WHERE ind.page_id=:#{#page.id}",
            nativeQuery = true)
    void deleteAllByPage(Page page);
}
