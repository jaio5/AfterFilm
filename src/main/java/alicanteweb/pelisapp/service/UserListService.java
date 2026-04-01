package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.entity.UserContentList;
import alicanteweb.pelisapp.repository.UserContentListRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserListService {

    public static final String FAVORITE  = "FAVORITE";
    public static final String WATCHLIST = "WATCHLIST";

    private final UserContentListRepository repo;
    private final UserRepository userRepository;

    /**
     * Toggles the item in the given list. Returns true if now active, false if removed.
     */
    @Transactional
    public boolean toggle(String username, String listType, String contentType,
                          Long contentId, String title, String poster) {
        User user = userRepository.findByUsername(username).orElseThrow();
        var existing = repo.findByUser_IdAndListTypeAndContentTypeAndContentId(
                user.getId(), listType, contentType, contentId);
        if (existing.isPresent()) {
            repo.delete(existing.get());
            log.debug("{} removed {} {}/{} from {}", username, contentType, contentId, title, listType);
            return false;
        }
        UserContentList item = new UserContentList();
        item.setUser(user);
        item.setListType(listType);
        item.setContentType(contentType);
        item.setContentId(contentId);
        item.setContentTitle(title);
        item.setContentPoster(poster);
        item.setAddedAt(Instant.now());
        repo.save(item);
        log.debug("{} added {} {}/{} to {}", username, contentType, contentId, title, listType);
        return true;
    }

    public boolean isInList(String username, String listType, String contentType, Long contentId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        return repo.existsByUser_IdAndListTypeAndContentTypeAndContentId(
                user.getId(), listType, contentType, contentId);
    }

    public List<UserContentList> getList(String username, String listType) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return repo.findByUser_IdAndListTypeOrderByAddedAtDesc(user.getId(), listType);
    }
}