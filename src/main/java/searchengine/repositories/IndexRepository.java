package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Index;
import searchengine.entities.Lemma;

import java.util.List;


@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Modifying
    @Query(value = "INSERT INTO indexes (page_id, lemma_id, lemma_rank) " +
            "VALUES (:#{#index.pageId}, :#{#index.lemmaId}, :#{#index.rank}) " +
            "ON DUPLICATE KEY UPDATE lemma_rank=:#{#index.rank}",
            nativeQuery = true)
    void insertIndex(Index index);

    @Query(value = "SELECT * FROM indexes WHERE page_id = :#{#pageId}",
            nativeQuery = true)
    List<Index> findAllByPageId(Integer pageId);

    List<Index> findAllByLemma(Lemma lemma);
}
