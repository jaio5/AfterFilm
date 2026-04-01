package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Message;
import alicanteweb.pelisapp.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :u1 AND m.receiver.id = :u2) OR " +
           "(m.sender.id = :u2 AND m.receiver.id = :u1) " +
           "ORDER BY m.sentAt ASC")
    List<Message> findConversation(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :u1 AND m.receiver.id = :u2) OR " +
           "(m.sender.id = :u2 AND m.receiver.id = :u1) " +
           "ORDER BY m.sentAt DESC")
    List<Message> findLastMessages(@Param("u1") Long u1, @Param("u2") Long u2, Pageable pageable);

    long countByReceiver_IdAndReadAtIsNull(Long receiverId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now " +
           "WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId AND m.readAt IS NULL")
    void markConversationAsRead(@Param("receiverId") Long receiverId,
                                @Param("senderId") Long senderId,
                                @Param("now") Instant now);

    @Query("SELECT DISTINCT m.receiver FROM Message m WHERE m.sender.id = :userId")
    List<User> findMessageReceivers(@Param("userId") Long userId);

    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.receiver.id = :userId")
    List<User> findMessageSenders(@Param("userId") Long userId);
}