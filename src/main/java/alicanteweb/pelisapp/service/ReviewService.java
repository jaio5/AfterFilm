package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.constants.AppConstants;
import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.entity.Movie;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.ReviewLike;
import alicanteweb.pelisapp.entity.TvShow;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.repository.MovieRepository;
import alicanteweb.pelisapp.repository.ReviewLikeRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.TvShowRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final BookRepository bookRepository;
    private final ModerationService moderationService;
    private final UserService userService;

    @Transactional
    public Review createReview(Long userId, Long movieId, String text, int stars) {
        String reviewText = normalizeReviewText(text);
        validateReviewInput(stars, reviewText);
        User user = findUserById(userId);
        Movie movie = findMovieById(movieId);
        checkUserBanStatus(user);
        runModerationSync(user, reviewText, movie.getTitle());
        Review saved = reviewRepository.save(buildReview(user, movie, reviewText, stars));
        runModerationAsync(saved);
        userService.onUserPostedReview(user.getId());
        log.info("✅ Reseña publicada - Usuario: {}, Película: {}, Estrellas: {}",
                user.getUsername(), movie.getTitle(), stars);
        return saved;
    }

    @Transactional
    public void likeReview(Long likerUserId, Long reviewId) {
        Review review = findReviewById(reviewId);
        User liker = findUserById(likerUserId);

        validateLikeOperation(liker, review);

        if (reviewLikeRepository.existsByUser_IdAndReview_Id(likerUserId, reviewId)) {
            log.debug("Usuario {} ya había dado like a la reseña {}", likerUserId, reviewId);
            return;
        }

        createReviewLike(liker, review);
        incrementLikesCount(review);

        // Actualizar logros del autor de la reseña
        userService.onUserReceivedLike(review.getUser().getId());

        log.info("Like añadido - Usuario: {}, Reseña: {}, Total likes: {}",
                liker.getUsername(), reviewId, review.getLikesCount() + 1);
    }

    public Page<Review> getReviewsByUsername(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return Page.empty();
        return reviewRepository.findAllByUser_Id(user.getId(), pageable);
    }

    public List<Review> getReviewsByMovieId(Long movieId) {
        return reviewRepository.findByMovieIdOrderByCreatedAtDesc(movieId);
    }

    public List<Review> getReviewsBySeriesId(Long seriesId) {
        if (!tvShowRepository.existsById(seriesId)) {
            throw new IllegalArgumentException("Serie no encontrada: " + seriesId);
        }
        return reviewRepository.findBySeriesIdOrderByCreatedAtDesc(seriesId);
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new IllegalArgumentException("Libro no encontrado: " + bookId);
        }
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
    }

    @Transactional
    public Review createSeriesReview(Long userId, Long seriesId, String text, int stars) {
        String reviewText = normalizeReviewText(text);
        validateReviewInput(stars, reviewText);
        User user = findUserById(userId);
        TvShow series = tvShowRepository.findById(seriesId)
                .orElseThrow(() -> new IllegalArgumentException("Serie no encontrada: " + seriesId));
        checkUserBanStatus(user);
        runModerationSync(user, reviewText, series.getTitle());
        Review saved = reviewRepository.save(buildReview(user, series, reviewText, stars));
        runModerationAsync(saved);
        userService.onUserPostedReview(user.getId());
        log.info("Reseña de serie publicada - Usuario: {}, Serie: {}, Estrellas: {}", user.getUsername(), series.getTitle(), stars);
        return saved;
    }

    @Transactional
    public Review createBookReview(Long userId, Long bookId, String text, int stars) {
        String reviewText = normalizeReviewText(text);
        validateReviewInput(stars, reviewText);
        User user = findUserById(userId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado: " + bookId));
        checkUserBanStatus(user);
        runModerationSync(user, reviewText, book.getTitle());
        Review saved = reviewRepository.save(buildReview(user, book, reviewText, stars));
        runModerationAsync(saved);
        userService.onUserPostedReview(user.getId());
        log.info("Reseña de libro publicada - Usuario: {}, Libro: {}, Estrellas: {}", user.getUsername(), book.getTitle(), stars);
        return saved;
    }

    private String normalizeReviewText(String text) {
        return text == null || text.trim().isEmpty() ? "" : text.trim();
    }

    /**
     * Valida la entrada de una reseña.
     * Permite texto vacío - solo se requieren las estrellas.
     */
    private void validateReviewInput(int stars, String text) {
        if (stars < AppConstants.MIN_STARS_RATING || stars > AppConstants.MAX_STARS_RATING) {
            throw new IllegalArgumentException(AppConstants.ERROR_INVALID_RATING);
        }

        // Permitir texto vacío - solo validar si hay texto
        if (text != null && text.length() > AppConstants.MAX_REVIEW_TEXT_LENGTH) {
            throw new IllegalArgumentException("El texto de la reseña es demasiado largo");
        }
    }

    /**
     * Busca un usuario por ID o lanza excepción.
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", userId);
                    return new IllegalArgumentException(AppConstants.ERROR_USER_NOT_FOUND + ": " + userId);
                });
    }

    /**
     * Busca una película por ID o lanza excepción.
     */
    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> {
                    log.error("Película no encontrada: {}", movieId);
                    return new IllegalArgumentException(AppConstants.ERROR_MOVIE_NOT_FOUND + ": " + movieId);
                });
    }

    /**
     * Busca una reseña por ID o lanza excepción.
     */
    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("Reseña no encontrada: {}", reviewId);
                    return new IllegalArgumentException(AppConstants.ERROR_REVIEW_NOT_FOUND + ": " + reviewId);
                });
    }

    private Review buildReview(User user, Movie movie, String text, int stars) {
        Review review = new Review();
        review.setUser(user);
        review.setMovie(movie);
        review.setText(text);
        review.setStars(stars);
        review.setCreatedAt(Instant.now());
        review.setLikesCount(0L);
        return review;
    }

    private Review buildReview(User user, TvShow series, String text, int stars) {
        Review review = new Review();
        review.setUser(user);
        review.setSeries(series);
        review.setText(text);
        review.setStars(stars);
        review.setCreatedAt(Instant.now());
        review.setLikesCount(0L);
        return review;
    }

    private Review buildReview(User user, Book book, String text, int stars) {
        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setText(text);
        review.setStars(stars);
        review.setCreatedAt(Instant.now());
        review.setLikesCount(0L);
        return review;
    }

    private void runModerationSync(User user, String text, String contentTitle) {
        if (text == null || text.trim().isEmpty()) {
            log.debug("📝 Reseña sin texto - Usuario: {}", user.getUsername());
            return;
        }
        try {
            log.debug("🛡️ Verificando contenido - Usuario: {}, Contenido: {}", user.getUsername(), contentTitle);
            ModerationService.ModerationResult result = moderationService.moderateContentSync(text);
            log.debug("✅ Contenido aprobado - Usuario: {}, Puntuación: {}",
                    user.getUsername(), String.format("%.2f", result.toxicityScore()));
        } catch (ModerationService.ContentModerationException e) {
            log.warn("❌ Contenido rechazado - Usuario: {}, Contenido: {}", user.getUsername(), contentTitle);
            String banMessage = applyProgressiveBan(user, e.isHarassmentIntent());
            throw new IllegalArgumentException("Tu comentario infringe las normas de la comunidad. " + banMessage);
        }
    }

    private void runModerationAsync(Review saved) {
        if (saved.getText() == null || saved.getText().trim().isEmpty()) {
            log.debug("📝 Reseña solo con estrellas - se omite moderación asíncrona, ID: {}", saved.getId());
            return;
        }
        moderationService.moderateReviewAsync(saved)
            .thenAccept(moderation -> log.debug("📊 Moderación asíncrona completada - ID: {}, Estado: {}",
                    saved.getId(), moderation.getStatus()))
            .exceptionally(ex -> {
                log.warn("⚠️ Error en moderación asíncrona: {}", ex.getMessage());
                return null;
            });
    }

    /**
     * Valida que se pueda dar like a la reseña.
     */
    private void validateLikeOperation(User liker, Review review) {
        if (review.getUser().getId().equals(liker.getId())) {
            log.warn("Usuario {} intentó dar like a su propia reseña {}", liker.getId(), review.getId());
            throw new IllegalArgumentException("No puedes dar like a tu propia reseña");
        }
    }

    /**
     * Crea un nuevo like para la reseña.
     */
    private void createReviewLike(User liker, Review review) {
        ReviewLike like = new ReviewLike();
        like.setReview(review);
        like.setUser(liker);
        like.setCreatedAt(Instant.now());
        reviewLikeRepository.save(like);
    }

    /**
     * Incrementa atómicamente el contador de likes de la reseña.
     */
    private void incrementLikesCount(Review review) {
        reviewRepository.incrementLikesCount(review.getId());
    }

    /**
     * Verifica si el usuario está baneado (permanente o temporal).
     * Si el ban temporal ha expirado, lo limpia automáticamente.
     */
    private void checkUserBanStatus(User user) {
        if (user.isBanned()) {
            throw new IllegalArgumentException("Tu cuenta ha sido suspendida permanentemente y no puedes publicar reseñas.");
        }
        if (user.getBannedUntil() != null) {
            if (Instant.now().isBefore(user.getBannedUntil())) {
                throw new IllegalArgumentException(
                    String.format("Tu cuenta está temporalmente suspendida hasta %s. Razón: %s",
                        user.getBannedUntil(), user.getBanReason() != null ? user.getBanReason() : "Infracción de normas"));
            } else {
                // Ban temporal expirado — limpiar automáticamente
                user.setBannedUntil(null);
                user.setBanReason(null);
                userRepository.save(user);
                log.info("Ban temporal expirado y eliminado - Usuario: {}", user.getUsername());
            }
        }
    }

    /**
     * Aplica el sistema de baneo progresivo tras detectar contenido inapropiado.
     * Progresión: advertencia → 1d → 3d → 7d → 30d → ban permanente
     */
    private String applyProgressiveBan(User user, boolean isHarassment) {
        int currentOffenses = user.getOffenseCount();
        user.setOffenseCount(currentOffenses + 1);

        String message = switch (currentOffenses) {
            case 0 -> "Esta es tu primera advertencia. Si reincides recibirás una suspensión temporal.";
            case 1 -> {
                user.setBannedUntil(Instant.now().plus(1, ChronoUnit.DAYS));
                user.setBanReason("Infracción reiterada de normas (2ª vez)");
                yield "Has sido suspendido temporalmente durante 1 día.";
            }
            case 2 -> {
                user.setBannedUntil(Instant.now().plus(3, ChronoUnit.DAYS));
                user.setBanReason("Infracción reiterada de normas (3ª vez)");
                yield "Has sido suspendido temporalmente durante 3 días.";
            }
            case 3 -> {
                user.setBannedUntil(Instant.now().plus(7, ChronoUnit.DAYS));
                user.setBanReason("Infracción reiterada de normas (4ª vez)");
                yield "Has sido suspendido temporalmente durante 7 días.";
            }
            case 4 -> {
                user.setBannedUntil(Instant.now().plus(30, ChronoUnit.DAYS));
                user.setBanReason("Infracción reiterada de normas (5ª vez)");
                yield "Has sido suspendido temporalmente durante 30 días.";
            }
            default -> {
                user.setBanned(true);
                user.setBanReason("Infracciones reiteradas de normas (" + (currentOffenses + 1) + "ª vez)");
                yield "Tu cuenta ha sido suspendida permanentemente por infracciones reiteradas de las normas.";
            }
        };

        userRepository.save(user);
        log.warn("⚠️ Sanción aplicada - Usuario: {}, Infracción #{}, Acoso: {}, Mensaje: {}",
                user.getUsername(), currentOffenses + 1, isHarassment, message);
        return message;
    }
}
