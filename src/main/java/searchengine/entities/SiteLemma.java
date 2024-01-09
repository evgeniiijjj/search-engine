package searchengine.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Entity
@Table(name = "site_lemmas")
public class SiteLemma implements Comparable<SiteLemma> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name = "site_id")
    @ManyToOne(cascade = CascadeType.MERGE)
    @Nonnull
    private Site site;

    @JoinColumn(name = "lemma_id")
    @ManyToOne(cascade = CascadeType.MERGE)
    @Nonnull
    private Lemma lemma;

    @Nonnull
    private Integer frequency;

    public SiteLemma(@Nonnull Site site,
                     @Nonnull Lemma lemma) {

        this.site = site;
        this.lemma = lemma;
        frequency = 1;
    }

    public int getSiteId() {

        return site.getId();
    }

    public int getLemmaId() {

        return lemma.getId();
    }


    @Override
    public int compareTo(SiteLemma o) {

        return frequency.compareTo(o.frequency);
    }
}
