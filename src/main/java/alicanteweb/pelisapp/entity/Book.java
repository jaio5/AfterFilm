package alicanteweb.pelisapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books", indexes = {
        @Index(columnList = "title"),
        @Index(columnList = "google_books_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_books_id", unique = true)
    private String googleBooksId;

    private String isbn;

    @Column(nullable = false)
    private String title;

    private String authors;

    private String publisher;

    private String publishedDate;

    @Column(length = 3000)
    private String description;

    private Integer pageCount;

    private String categories;

    private String language;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Review> reviews = new HashSet<>();
}
