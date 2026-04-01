package alicanteweb.pelisapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tv_shows", indexes = {
        @Index(columnList = "title"),
        @Index(columnList = "tmdb_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TvShow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    private String originalTitle;

    @Column(length = 2000)
    private String overview;

    private String posterPath;

    @Column(name = "poster_local_path")
    private String posterLocalPath;

    private String backdropPath;

    private LocalDate firstAirDate;

    private Integer numberOfSeasons;

    private Integer numberOfEpisodes;

    private String genres;

    private String status;

    private String language;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tv_show_actor",
            joinColumns = @JoinColumn(name = "tv_show_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id"))
    private Set<Actor> actors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tv_show_director",
            joinColumns = @JoinColumn(name = "tv_show_id"),
            inverseJoinColumns = @JoinColumn(name = "director_id"))
    private Set<Director> directors = new HashSet<>();

    @OneToMany(mappedBy = "series", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Review> reviews = new HashSet<>();
}
