package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.UserContentList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContentListRepository extends JpaRepository<UserContentList, Long> {

    boolean existsByUser_IdAndListTypeAndContentTypeAndContentId(
            Long userId, String listType, String contentType, Long contentId);

    Optional<UserContentList> findByUser_IdAndListTypeAndContentTypeAndContentId(
            Long userId, String listType, String contentType, Long contentId);

    List<UserContentList> findByUser_IdAndListTypeOrderByAddedAtDesc(Long userId, String listType);

    long countByUser_IdAndListType(Long userId, String listType);

    @Modifying
    @Transactional
    void deleteByUser_Id(Long userId);
}
