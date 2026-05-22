package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByGoogleBooksId(String googleBooksId);
    List<Book> findByTitleContainingIgnoreCase(String title);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(COALESCE(b.authors, '')) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchByTitleOrAuthors(@Param("query") String query, Pageable pageable);
    Page<Book> findAll(Pageable pageable);

    Page<Book> findByCategoriesContainingIgnoreCase(String category, Pageable pageable);

    @Query("SELECT b.categories FROM Book b WHERE b.categories IS NOT NULL AND b.categories <> ''")
    List<String> findAllCategoryStrings();

    @Query(value = "SELECT * FROM books WHERE categories ~* :pattern",
           countQuery = "SELECT COUNT(*) FROM books WHERE categories ~* :pattern",
           nativeQuery = true)
    Page<Book> findByCategoriesMatchingPattern(@Param("pattern") String pattern, Pageable pageable);

    @Query("SELECT b.id, COALESCE(AVG(CAST(r.stars AS double)), null), COUNT(r.id) " +
           "FROM Book b LEFT JOIN b.reviews r " +
           "WHERE b.id IN :ids GROUP BY b.id")
    List<Object[]> findRatingStatsByIds(@Param("ids") List<Long> ids);
}
