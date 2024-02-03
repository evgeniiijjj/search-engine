package searchengine.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "indexes")
public class Index implements Comparable<Index> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @JoinColumn(name = "page_id",
            nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Page page;
    @Column(nullable = false)
    private String lemma;
    @Column(name = "lemma_rank",
            nullable = false)
    private Float rank;

    public Index(@Nonnull Page page,
                 @Nonnull String lemma,
                 @Nonnull Float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    public int getPageId() {
        return page.getId();
    }

    @Override
    public int hashCode() {
        return (page.getSite().getUrl() +
                page.getPath() +
                lemma).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(Index.class)) {
            return false;
        }
        Index i = (Index) o;
        return page.equals(i.page) &&
                lemma.equals(i.lemma);
    }

    @Override
    public int compareTo(Index o) {
        return rank.compareTo(o.rank);
    }
}
