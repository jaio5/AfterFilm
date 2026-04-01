package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Following;
import alicanteweb.pelisapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowingRepository extends JpaRepository<Following, Long> {

    boolean existsByFollowerAndFollowed(User follower, User followed);

    Optional<Following> findByFollowerAndFollowed(User follower, User followed);

    List<Following> findByFollower(User follower);

    List<Following> findByFollowed(User followed);

    long countByFollowed(User followed);

    long countByFollower(User follower);
}