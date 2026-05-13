package alicanteweb.pelisapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "chat_conversation_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "partner_id"}),
        indexes = {
                @Index(columnList = "owner_id"),
                @Index(columnList = "partner_id"),
                @Index(columnList = "starred")
        })
@Getter
@Setter
@NoArgsConstructor
public class ChatConversationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private User partner;

    @Column(nullable = false)
    private boolean starred;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
