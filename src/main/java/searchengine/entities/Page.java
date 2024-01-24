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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pages")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Site site;
    @Column(name = "page_path",
            unique = true,
            nullable = false)
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(name = "page_content",
            nullable = false)
    private String content;

    public Page(@Nonnull Site site,
                @Nonnull String path) {
        this.site = site;
        this.path = path;
    }

    public int getSiteId() {
        return site.getId();
    }

    @Override
    public int hashCode() {
        return (site.getUrl() + path).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(Page.class)) {
            return false;
        }
        Page p = (Page) o;
        return (site.getUrl() + path)
                .equals(p.site.getUrl() + p.path);
    }
}
