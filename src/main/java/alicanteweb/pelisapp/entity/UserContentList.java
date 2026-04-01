package alicanteweb.pelisapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores user-curated content lists: FAVORITE and WATCHLIST.
 * Supports movies, series and books via contentType + contentId.
 */
@Entity
@Table(name = "user_content_list", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "list_type", "content_type", "content_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class UserContentList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** "FAVORITE" or "WATCHLIST" */
    @Column(name = "list_type", nullable = false, length = 20)
    private String listType;

    /** "movie", "series" or "book" */
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "content_title", length = 500)
    private String contentTitle;

    @Column(name = "content_poster", length = 500)
    private String contentPoster;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;
}