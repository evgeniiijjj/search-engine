package searchengine.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


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

    @Column(unique = true, nullable = false)
    private String lemma;

    @ManyToMany
    @JoinTable(name = "site_lemmas",
            joinColumns = { @JoinColumn(name = "lemma") },
            inverseJoinColumns = { @JoinColumn(name = "site") })
    private Set<Site> sites = new HashSet<>();


    public Lemma(@Nonnull String lemma) {

        this.lemma = lemma;
    }

    @Override
    public int hashCode() {

        return lemma.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (!o.getClass().equals(Lemma.class)) {

            return false;
        }
        Lemma l = (Lemma) o;
        return lemma.equals(l.lemma);
    }
}
