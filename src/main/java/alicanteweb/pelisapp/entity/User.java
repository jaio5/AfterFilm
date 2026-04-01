package alicanteweb.pelisapp.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario", indexes = {
        @Index(columnList = "username"),
        @Index(columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String username;

    @Column(unique = true, length = 200)
    private String email;

    @Column(nullable = false)
    private String password;

    private String displayName;

    private Instant registeredAt;

    @Column(name = "email_confirmed", nullable = false)
    private boolean emailConfirmed = false;

    @Column(name = "banned", nullable = false)
    private boolean banned = false;

    /** Number of times this user has been sanctioned by the moderation system. */
    @Column(name = "offense_count", columnDefinition = "INT NOT NULL DEFAULT 0")
    private int offenseCount = 0;

    /** When a temporary ban expires. Null means no active temp ban. */
    @Column(name = "banned_until")
    private Instant bannedUntil;

    /** Reason for the most recent sanction. */
    @Column(name = "ban_reason", length = 500)
    private String banReason;

    private Integer criticLevel = 0; // computed based on likes per review

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "follower", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Following> following = new HashSet<>();

    @OneToMany(mappedBy = "followed", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Following> followers = new HashSet<>();

    // Replaced ManyToMany archievements with explicit entity to hold metadata
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<UsuarioArchivement> usuarioArchievements = new HashSet<>();

    // Roles (ManyToMany)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Set<Role> roles = new HashSet<>();

    // Tags / Badges (ManyToMany)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_etiquetas",
            joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id", referencedColumnName = "id"))
    private Set<Tag> tags = new HashSet<>();

    // Defensive getters for collections to avoid exposing internal mutable sets
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles == null ? Collections.emptySet() : new HashSet<>(roles));
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags == null ? Collections.emptySet() : new HashSet<>(tags));
    }

    // Helper methods to manipulate collections safely
    public void addRole(Role role) {
        if (this.roles == null) this.roles = new HashSet<>();
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        if (this.roles == null) return;
        this.roles.remove(role);
    }

    public void addTag(Tag tag) {
        if (this.tags == null) this.tags = new HashSet<>();
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        if (this.tags == null) return;
        this.tags.remove(tag);
    }
}
