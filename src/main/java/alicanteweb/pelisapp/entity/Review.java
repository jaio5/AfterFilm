package alicanteweb.pelisapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "review", indexes = {
        @Index(columnList = "movie_id"),
        @Index(columnList = "series_id"),
        @Index(columnList = "book_id"),
        @Index(columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = true)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = true)
    private TvShow series;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = true)
    private Book book;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Integer stars; // 1..5

    private Instant createdAt;
    private Instant updatedAt;

    @Column(nullable = false)
    private Long likesCount = 0L;

    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ReviewLike> likes = new HashSet<>();

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL)
    private CommentModeration moderation;

    public String getContentType() {
        if (series != null) return "series";
        if (book != null) return "book";
        return "movie";
    }
}
