package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.ReviewReply;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.ReviewReplyRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewReplyService {
    private static final int MAX_REPLY_LENGTH = 1000;

    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReviewReply> getReplies(Long reviewId) {
        return reviewReplyRepository.findByReview_IdOrderByCreatedAtAsc(reviewId);
    }

    @Transactional
    public ReviewReply createReply(Long reviewId, Long userId, String text) {
        String replyText = normalizeText(text);
        if (replyText.isBlank()) {
            throw new IllegalArgumentException("La respuesta no puede estar vacía");
        }
        if (replyText.length() > MAX_REPLY_LENGTH) {
            throw new IllegalArgumentException("La respuesta es demasiado larga");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.isBanned()) {
            throw new SecurityException("Usuario baneado");
        }

        ReviewReply reply = new ReviewReply();
        reply.setReview(review);
        reply.setUser(user);
        reply.setText(replyText);
        reply.setCreatedAt(Instant.now());
        return reviewReplyRepository.save(reply);
    }

    @Transactional
    public void deleteOwnReply(Long replyId, Long userId) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Respuesta no encontrada"));
        if (reply.getUser() == null || !reply.getUser().getId().equals(userId)) {
            throw new SecurityException("Solo puedes borrar tus propias respuestas");
        }
        reviewReplyRepository.delete(reply);
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }
}
