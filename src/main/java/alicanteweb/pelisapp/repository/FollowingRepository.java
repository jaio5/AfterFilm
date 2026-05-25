package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Following;
import alicanteweb.pelisapp.entity.User;
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
public interface FollowingRepository extends JpaRepository<Following, Long> {

    boolean existsByFollowerAndFollowed(User follower, User followed);

    Optional<Following> findByFollowerAndFollowed(User follower, User followed);

    List<Following> findByFollower(User follower);

    List<Following> findByFollower(User follower, Pageable pageable);

    List<Following> findByFollowed(User followed);

    List<Following> findByFollowed(User followed, Pageable pageable);

    long countByFollowed(User followed);

    long countByFollower(User follower);

    @Modifying
    @Transactional
    @Query("DELETE FROM Following f WHERE f.follower.id = :userId OR f.followed.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
