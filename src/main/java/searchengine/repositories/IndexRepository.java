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
    void deleteAllByPage(Page page);

    @Query(value = "SELECT * FROM indexes WHERE lemma_id=:#{#lemma.id} " +
            "ORDER BY lemma_rank DESC LIMIT :#{#limit}", nativeQuery = true)
    List<Index> findAllByLemmaOrderByRankDescLimit(Lemma lemma, int limit);

    List<Index> findAllByPage(Page page);
}
