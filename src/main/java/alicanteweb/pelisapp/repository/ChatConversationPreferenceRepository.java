package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.ChatConversationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationPreferenceRepository extends JpaRepository<ChatConversationPreference, Long> {
    Optional<ChatConversationPreference> findByOwner_IdAndPartner_Id(Long ownerId, Long partnerId);
    List<ChatConversationPreference> findAllByOwner_Id(Long ownerId);
}
