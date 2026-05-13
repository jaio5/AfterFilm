package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.ChatConversationDTO;
import alicanteweb.pelisapp.dto.MessageDTO;
import alicanteweb.pelisapp.dto.SendMessageRequest;
import alicanteweb.pelisapp.entity.ChatConversationPreference;
import alicanteweb.pelisapp.entity.Message;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.ChatConversationPreferenceRepository;
import alicanteweb.pelisapp.repository.MessageRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatConversationPreferenceRepository preferenceRepository;

    @Transactional
    public MessageDTO sendMessage(String senderUsername, String receiverUsername, SendMessageRequest req) {
        User sender = userRepository.findByUsername(senderUsername).orElseThrow();
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + receiverUsername));
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("No puedes chatear contigo mismo");
        }

        ChatConversationPreference senderPreference = getOrCreatePreference(sender, receiver);
        senderPreference.setDeletedAt(null);
        preferenceRepository.save(senderPreference);

        Message msg = new Message();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(req.getContent());
        msg.setSentAt(Instant.now());
        msg.setSharedContentType(req.getSharedContentType());
        msg.setSharedContentId(req.getSharedContentId());
        msg.setSharedContentTitle(req.getSharedContentTitle());
        msg.setSharedContentPoster(req.getSharedContentPoster());
        return toDTO(messageRepository.save(msg));
    }

    @Transactional
    public List<MessageDTO> getConversation(String currentUsername, String otherUsername) {
        User current = userRepository.findByUsername(currentUsername).orElseThrow();
        User other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + otherUsername));
        ChatConversationPreference preference = getPreference(current.getId(), other.getId()).orElse(null);
        Instant deletedAt = preference != null ? preference.getDeletedAt() : null;
        messageRepository.markConversationAsRead(current.getId(), other.getId(), Instant.now());
        return messageRepository.findVisibleConversation(current.getId(), other.getId(), deletedAt)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return 0;
        return getConversationPartners(user).stream()
                .mapToLong(partner -> {
                    ChatConversationPreference preference = getPreference(user.getId(), partner.getId()).orElse(null);
                    Instant deletedAt = preference != null ? preference.getDeletedAt() : null;
                    return messageRepository.countVisibleUnreadFromPartner(user.getId(), partner.getId(), deletedAt);
                })
                .sum();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationDTO> getChatPartners(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return getConversationPartners(user).stream()
                .map(partner -> toConversationDTO(user, partner))
                .filter(conversation -> conversation.lastSentAt() != null)
                .sorted(Comparator
                        .comparing(ChatConversationDTO::starred).reversed()
                        .thenComparing(ChatConversationDTO::lastSentAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatConversationDTO setStarred(String username, String partnerUsername, boolean starred) {
        User owner = userRepository.findByUsername(username).orElseThrow();
        User partner = userRepository.findByUsername(partnerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + partnerUsername));
        ChatConversationPreference preference = getOrCreatePreference(owner, partner);
        preference.setStarred(starred);
        preferenceRepository.save(preference);
        return toConversationDTO(owner, partner);
    }

    @Transactional
    public void deleteConversation(String username, String partnerUsername) {
        User owner = userRepository.findByUsername(username).orElseThrow();
        User partner = userRepository.findByUsername(partnerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + partnerUsername));
        ChatConversationPreference preference = getOrCreatePreference(owner, partner);
        preference.setDeletedAt(Instant.now());
        preferenceRepository.save(preference);
    }

    private Set<User> getConversationPartners(User user) {
        Set<User> partners = new HashSet<>();
        partners.addAll(messageRepository.findMessageReceivers(user.getId()));
        partners.addAll(messageRepository.findMessageSenders(user.getId()));
        return partners;
    }

    private ChatConversationDTO toConversationDTO(User owner, User partner) {
        ChatConversationPreference preference = getPreference(owner.getId(), partner.getId()).orElse(null);
        Instant deletedAt = preference != null ? preference.getDeletedAt() : null;
        Message latest = messageRepository.findLatestVisibleMessage(
                        owner.getId(), partner.getId(), deletedAt, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
        long unread = messageRepository.countVisibleUnreadFromPartner(owner.getId(), partner.getId(), deletedAt);
        return new ChatConversationDTO(
                partner.getUsername(),
                partner.getDisplayName() != null ? partner.getDisplayName() : partner.getUsername(),
                preference != null && preference.isStarred(),
                unread,
                latest != null ? latest.getContent() : null,
                latest != null ? latest.getSentAt() : null
        );
    }

    private Optional<ChatConversationPreference> getPreference(Long ownerId, Long partnerId) {
        return preferenceRepository.findByOwner_IdAndPartner_Id(ownerId, partnerId);
    }

    private ChatConversationPreference getOrCreatePreference(User owner, User partner) {
        return getPreference(owner.getId(), partner.getId()).orElseGet(() -> {
            ChatConversationPreference preference = new ChatConversationPreference();
            preference.setOwner(owner);
            preference.setPartner(partner);
            return preference;
        });
    }

    private MessageDTO toDTO(Message m) {
        return new MessageDTO(
                m.getId(),
                m.getSender().getUsername(),
                m.getReceiver().getUsername(),
                m.getContent(),
                m.getSentAt(),
                m.isRead(),
                m.getSharedContentType(),
                m.getSharedContentId(),
                m.getSharedContentTitle(),
                m.getSharedContentPoster()
        );
    }
}
