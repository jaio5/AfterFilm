package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByUser_Id(Long userId);
    List<Review> findAllByUser_Id(Long userId);
    Page<Review> findAllByUser_Id(Long userId, Pageable pageable);
    List<Review> findByMovieId(Long movieId);
    List<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId);
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    List<Review> findBySeriesIdOrderByCreatedAtDesc(Long seriesId);
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);
    Optional<Review> findByUserIdAndSeriesId(Long userId, Long seriesId);
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    @Modifying
    @Query("UPDATE Review r SET r.likesCount = r.likesCount + 1 WHERE r.id = :id")
    void incrementLikesCount(@Param("id") Long id);

    @Query("SELECT r FROM Review r WHERE r.user.id IN :userIds ORDER BY r.createdAt DESC")
    List<Review> findFeedByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);
}
