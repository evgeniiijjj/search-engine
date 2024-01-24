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

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Site site;
    @Column(nullable = false)
    private String lemma;
    @Column(nullable = false)
    private Integer frequency;

    public Lemma(@Nonnull Site site,
                 @Nonnull String lemma) {
        this.site = site;
        this.lemma = lemma;
        frequency = 1;
    }

    public int getSiteId() {
        return site.getId();
    }

    @Override
    public int hashCode() {
        return (lemma + site.getUrl()).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(Lemma.class)) {
            return false;
        }
        Lemma l = (Lemma) o;
        return (lemma + site.getUrl())
                .equals(l.lemma + l.site.getUrl());
    }
}
