package alicanteweb.pelisapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
        @Index(columnList = "sender_id"),
        @Index(columnList = "receiver_id"),
        @Index(columnList = "sent_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    /** Optional shared content — set when the message includes a movie/series/book recommendation */
    @Column(name = "shared_content_type", length = 20)
    private String sharedContentType; // "movie", "series", "book"

    @Column(name = "shared_content_id")
    private Long sharedContentId;

    @Column(name = "shared_content_title", length = 500)
    private String sharedContentTitle;

    @Column(name = "shared_content_poster", length = 500)
    private String sharedContentPoster;

    public boolean isRead() {
        return readAt != null;
    }
}