package searchengine.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
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

    @JoinColumn(name = "page_id")
    @ManyToOne(cascade = CascadeType.MERGE)
    @Nonnull
    private Page page;

    @JoinColumn(name = "lemma_id")
    @ManyToOne(cascade = CascadeType.MERGE)
    @Nonnull
    private Lemma lemma;

    @Nonnull
    @Column(name = "lemma_rank")
    private Float rank;


    public Index(@Nonnull Page page,
                 @Nonnull Lemma lemma,
                 @Nonnull Float rank) {

        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    public int getPageId() {

        return page.getId();
    }

    public int getLemmaId() {

        return lemma.getId();
    }

    @Override
    public int hashCode() {
        return (page.getSite().getUrl() +
                page.getPath() +
                lemma.getLemma()).hashCode();
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
