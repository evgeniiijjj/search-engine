package searchengine.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.enums.Statuses;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "site_status",
            columnDefinition = "enum",
            nullable = false)
    private volatile Statuses status;

    @Column(name = "status_time",
            nullable = false)
    private volatile Instant statusTime;

    @Column(name = "last_error")
    private volatile String lastError;

    @Column(name = "site_url",
            unique = true,
            nullable = false)
    private String url;

    @Column(name = "site_name",
            nullable = false)
    private String name;


    public Site(@Nonnull String url) {

        this.url = url;
    }


    @Override
    public int hashCode() {

        return url.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (!this.getClass()
                .equals(o.getClass())) {
            return false;
        }
        Site s = (Site) o;
        return url.equals(s.url);
    }

    public String getStatus() {

        return status.name();
    }
}
