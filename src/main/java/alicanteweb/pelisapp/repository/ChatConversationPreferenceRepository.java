package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.ChatConversationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationPreferenceRepository extends JpaRepository<ChatConversationPreference, Long> {
    Optional<ChatConversationPreference> findByOwner_IdAndPartner_Id(Long ownerId, Long partnerId);
    List<ChatConversationPreference> findAllByOwner_Id(Long ownerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatConversationPreference p WHERE p.owner.id = :userId OR p.partner.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
