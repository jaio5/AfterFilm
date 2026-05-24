package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.CommentModeration;
import alicanteweb.pelisapp.entity.CommentModeration.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentModerationRepository extends JpaRepository<CommentModeration, Long> {

    Optional<CommentModeration> findByReview_Id(Long reviewId);

    Page<CommentModeration> findByStatus(ModerationStatus status, Pageable pageable);

    @Query("SELECT cm FROM CommentModeration cm WHERE cm.status = :status ORDER BY cm.createdAt ASC")
    List<CommentModeration> findByStatusOrderByCreatedAsc(ModerationStatus status);

    long countByStatus(ModerationStatus status);

    @Query("SELECT cm FROM CommentModeration cm WHERE cm.aiProcessed = false AND cm.status = 'PENDING'")
    List<CommentModeration> findPendingAiModeration();

    @Modifying
    @Transactional
    @Query("UPDATE CommentModeration cm SET cm.reviewedBy = null WHERE cm.reviewedBy.id = :userId")
    void clearReviewer(@Param("userId") Long userId);
}
