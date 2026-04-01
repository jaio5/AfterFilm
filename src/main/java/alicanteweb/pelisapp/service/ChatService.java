package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.MessageDTO;
import alicanteweb.pelisapp.dto.SendMessageRequest;
import alicanteweb.pelisapp.dto.UserPublicDTO;
import alicanteweb.pelisapp.entity.Message;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.MessageRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageDTO sendMessage(String senderUsername, String receiverUsername, SendMessageRequest req) {
        User sender = userRepository.findByUsername(senderUsername).orElseThrow();
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + receiverUsername));
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
        messageRepository.markConversationAsRead(current.getId(), other.getId(), Instant.now());
        return messageRepository.findConversation(current.getId(), other.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return 0;
        return messageRepository.countByReceiver_IdAndReadAtIsNull(user.getId());
    }

    public List<UserPublicDTO> getChatPartners(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Set<User> partners = new HashSet<>();
        partners.addAll(messageRepository.findMessageReceivers(user.getId()));
        partners.addAll(messageRepository.findMessageSenders(user.getId()));
        return partners.stream()
                .map(u -> new UserPublicDTO(u.getId(), u.getUsername(),
                        u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
                        0, 0, 0, false))
                .collect(Collectors.toList());
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