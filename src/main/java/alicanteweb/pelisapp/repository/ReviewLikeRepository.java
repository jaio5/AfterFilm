package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike,Long> {
    boolean existsByUser_IdAndReview_Id(Long userId, Long reviewId);
    long countByReview_User_Id(Long userId);
    long countByReview_IdIn(List<Long> reviewIds);
    List<ReviewLike> findAllByReview_IdIn(List<Long> reviewIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReviewLike rl WHERE rl.review.id = :reviewId")
    void deleteByReview_Id(@Param("reviewId") Long reviewId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReviewLike rl WHERE rl.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}
