package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.ProfileBadgeDTO;
import alicanteweb.pelisapp.dto.UserPublicDTO;
import alicanteweb.pelisapp.dto.UserProfileSummaryDTO;
import alicanteweb.pelisapp.entity.Archivement;
import alicanteweb.pelisapp.entity.Following;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.Role;
import alicanteweb.pelisapp.entity.Tag;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.entity.UsuarioArchivement;
import alicanteweb.pelisapp.repository.FollowingRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialService {

    private final UserRepository userRepository;
    private final FollowingRepository followingRepository;
    private final ReviewRepository reviewRepository;
    private final UserListService userListService;

    @Transactional(readOnly = true)
    public UserPublicDTO getPublicProfile(String username, String currentUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        return toPublicDTO(user, resolveCurrentUser(currentUsername));
    }

    @Transactional(readOnly = true)
    public UserProfileSummaryDTO getProfileSummary(String username, String currentUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        User currentUser = resolveCurrentUser(currentUsername);
        List<Review> reviews = reviewRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId());
        double averageRating = reviews.stream()
                .mapToInt(Review::getStars)
                .average()
                .orElse(0.0);
        long followersCount = followingRepository.countByFollowed(user);
        long followingCount = followingRepository.countByFollower(user);
        boolean isFollowing = currentUser != null && followingRepository.existsByFollowerAndFollowed(currentUser, user);
        String displayName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername();
        return new UserProfileSummaryDTO(
                user.getId(),
                user.getUsername(),
                displayName,
                user.getCriticLevel(),
                followersCount,
                followingCount,
                reviews.size(),
                averageRating,
                userListService.countList(username, UserListService.FAVORITE),
                userListService.countList(username, UserListService.WATCHLIST),
                isFollowing,
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getTags().stream().map(this::toTagBadge).toList(),
                user.getUsuarioArchievements().stream().map(this::toAchievementBadge).toList());
    }

    @Transactional
    public void follow(String followerUsername, String followedUsername) {
        if (followerUsername.equals(followedUsername)) {
            throw new IllegalArgumentException("No puedes seguirte a ti mismo");
        }
        User follower = userRepository.findByUsername(followerUsername).orElseThrow();
        User followed = userRepository.findByUsername(followedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + followedUsername));
        if (!followingRepository.existsByFollowerAndFollowed(follower, followed)) {
            Following f = new Following();
            f.setFollower(follower);
            f.setFollowed(followed);
            f.setCreatedAt(Instant.now());
            followingRepository.save(f);
            log.info("{} ahora sigue a {}", followerUsername, followedUsername);
        }
    }

    @Transactional
    public void unfollow(String followerUsername, String followedUsername) {
        User follower = userRepository.findByUsername(followerUsername).orElseThrow();
        User followed = userRepository.findByUsername(followedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + followedUsername));
        followingRepository.findByFollowerAndFollowed(follower, followed)
                .ifPresent(followingRepository::delete);
        log.info("{} dejó de seguir a {}", followerUsername, followedUsername);
    }

    @Transactional(readOnly = true)
    public List<UserPublicDTO> getFollowers(String username, String currentUsername) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return followingRepository.findByFollowed(user, PageRequest.of(0, 100)).stream()
                .map(f -> toCompactPublicDTO(f.getFollower()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPublicDTO> getFollowing(String username, String currentUsername) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return followingRepository.findByFollower(user, PageRequest.of(0, 100)).stream()
                .map(f -> toCompactPublicDTO(f.getFollowed()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPublicDTO> searchUsers(String query, String currentUsername) {
        User currentUser = resolveCurrentUser(currentUsername);
        return userRepository.findByUsernameContainingIgnoreCase(query, PageRequest.of(0, 20))
                .getContent().stream()
                .filter(u -> !u.getUsername().equals(currentUsername))
                .map(u -> toPublicDTO(u, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPublicDTO> getSuggestedUsers(String currentUsername) {
        User currentUser = resolveCurrentUser(currentUsername);
        return userRepository.findAll(PageRequest.of(0, 20))
                .getContent().stream()
                .filter(u -> currentUsername == null || !u.getUsername().equals(currentUsername))
                .map(u -> toPublicDTO(u, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Review> getUserReviews(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return reviewRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public List<Review> getUserReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return reviewRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<Review> getFeed(String username, int page, int size) {
        User user = userRepository.findByUsername(username).orElseThrow();
        List<Long> followedIds = followingRepository.findByFollower(user).stream()
                .map(f -> f.getFollowed().getId())
                .collect(Collectors.toList());
        if (followedIds.isEmpty()) return List.of();
        return reviewRepository.findFeedByUserIds(followedIds, PageRequest.of(page, size));
    }

    private UserPublicDTO toPublicDTO(User user, User currentUser) {
        long followersCount = followingRepository.countByFollowed(user);
        long followingCount = followingRepository.countByFollower(user);
        long reviewCount = reviewRepository.countByUser_Id(user.getId());
        boolean isFollowing = currentUser != null && followingRepository.existsByFollowerAndFollowed(currentUser, user);
        return buildPublicDTO(user, followersCount, followingCount, reviewCount, isFollowing);
    }

    private UserPublicDTO toCompactPublicDTO(User user) {
        return buildPublicDTO(user, 0, 0, 0, false);
    }

    private UserPublicDTO buildPublicDTO(User user, long followersCount, long followingCount, long reviewCount, boolean isFollowing) {
        String displayName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername();
        return new UserPublicDTO(user.getId(), user.getUsername(), displayName,
                followersCount, followingCount, reviewCount, isFollowing);
    }

    private User resolveCurrentUser(String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(currentUsername).orElse(null);
    }

    private ProfileBadgeDTO toTagBadge(Tag tag) {
        return new ProfileBadgeDTO(
                tag.getId(),
                tag.getCode(),
                tag.getName(),
                tag.getDescription(),
                tag.getIconUrl(),
                tag.getCreatedAt(),
                null);
    }

    private ProfileBadgeDTO toAchievementBadge(UsuarioArchivement userAchievement) {
        Archivement achievement = userAchievement.getArchivement();
        return new ProfileBadgeDTO(
                achievement.getId(),
                achievement.getCode(),
                achievement.getName(),
                achievement.getDescription(),
                achievement.getIconUrl(),
                userAchievement.getAwardedAt(),
                userAchievement.isPinnedToProfile());
    }
}
