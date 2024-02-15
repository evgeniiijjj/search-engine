package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entities.Index;
import searchengine.entities.Page;
import searchengine.entities.Site;
import java.util.List;

public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query(value = "SELECT * FROM indexes WHERE lemma=:#{#lemma} " +
            "GROUP BY page_id ORDER BY lemma_rank DESC LIMIT :#{#limit}",
            nativeQuery = true)
    List<Index> findByLemmaOrderByRankDescLimit(String lemma, int limit);

    @Query(value = "SELECT * FROM indexes WHERE lemma=:#{#lemma} AND page_id IN " +
            "(SELECT p.id FROM pages p WHERE p.site_id=:#{#site.id}) " +
            "GROUP BY page_id ORDER BY lemma_rank DESC LIMIT :#{#limit}",
            nativeQuery = true)
    List<Index> findByLemmaAndSiteOrderByRankDescLimit(String lemma, Site site, int limit);

    @Query(value = "SELECT COUNT(DISTINCT(lemma)) FROM indexes",
            nativeQuery = true)
    long countDistinctLemma();

    @Query(value = "SELECT COUNT(DISTINCT lemma) FROM indexes WHERE page_id IN " +
            "(SELECT id FROM pages WHERE site_id=:#{#site.id})",
            nativeQuery = true)
    long lemmaCountBySite(Site site);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM indexes WHERE page_id IN " +
            "(SELECT id FROM pages WHERE site_id=:#{#site.id})",
            nativeQuery = true)
    void deleteOldIndexesBySite(Site site);

    List<Index> findAllByPage(Page page);
}
