package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.ReviewReply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<ReviewReply> findByReview_IdOrderByCreatedAtAsc(Long reviewId);

    @EntityGraph(attributePaths = {"user"})
    List<ReviewReply> findByReview_IdInOrderByCreatedAtAsc(Collection<Long> reviewIds);
}
