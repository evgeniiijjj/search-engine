package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Index;
import searchengine.entities.Lemma;
import searchengine.entities.Page;
import searchengine.entities.Site;
import java.util.List;

public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query(value = "SELECT * FROM indexes WHERE lemma_id=:#{#lemma.id} " +
            "ORDER BY lemma_rank DESC LIMIT :#{#limit}",
            nativeQuery = true)
    List<Index> findAllByLemmaOrderByRankDescLimit(Lemma lemma, int limit);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM indexes ind WHERE ind.page_id IN " +
            "(SELECT p.id FROM pages p WHERE p.site_id=:#{#site.id})",
            nativeQuery = true)
    void deleteAllBySite(Site site);

    void deleteAllByPage(Page page);

    List<Index> findAllByPage(Page page);
}
