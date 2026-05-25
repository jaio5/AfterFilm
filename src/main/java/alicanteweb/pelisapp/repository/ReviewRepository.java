package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    Page<Review> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    @Query("SELECT r FROM Review r WHERE r.id = :id")
    Optional<Review> findByIdWithContent(@Param("id") Long id);

    long countByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findAllByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    Page<Review> findAllByUser_Id(Long userId, Pageable pageable);

    List<Review> findByMovieId(Long movieId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findAllByUserIdAndMovieIdOrderByCreatedAtDesc(Long userId, Long movieId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findBySeriesIdOrderByCreatedAtDesc(Long seriesId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    Optional<Review> findByUserIdAndSeriesId(Long userId, Long seriesId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findAllByUserIdAndSeriesIdOrderByCreatedAtDesc(Long userId, Long seriesId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    List<Review> findAllByUserIdAndBookIdOrderByCreatedAtDesc(Long userId, Long bookId);

    @Modifying
    @Query("UPDATE Review r SET r.likesCount = r.likesCount + 1 WHERE r.id = :id")
    void incrementLikesCount(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "movie", "series", "book"})
    @Query("SELECT r FROM Review r WHERE r.user.id IN :userIds ORDER BY r.createdAt DESC")
    List<Review> findFeedByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);
}
