package alicanteweb.pelisapp.controller.web;

import alicanteweb.pelisapp.dto.ContentStats;
import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.entity.Review;
import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.repository.ReviewRepository;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BookViewController {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookService bookService;

    @GetMapping("/libro/{id}")
    public String bookDetail(@PathVariable Long id, Model model, Authentication auth) {
        try {
            Book book = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado"));
            List<Review> reviews = reviewRepository.findByBookIdOrderByCreatedAtDesc(book.getId());
            ContentStats stats = calculateStats(reviews);
            Review userReview = null;
            User currentUser = null;
            boolean isAuthenticated = auth != null && auth.isAuthenticated();
            if (isAuthenticated) {
                currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
                if (currentUser != null) {
                    userReview = reviewRepository.findByUserIdAndBookId(currentUser.getId(), book.getId()).orElse(null);
                }
            }
            boolean isAdmin = isAuthenticated && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                model.addAttribute("coverUrl", bookService.displayCoverUrl(book.getCoverUrl()));
            model.addAttribute("book", book);
            model.addAttribute("reviews", reviews);
            model.addAttribute("stats", stats);
            model.addAttribute("userReview", userReview);
            model.addAttribute("canReview", isAuthenticated && userReview == null);
            model.addAttribute("isAuthenticated", isAuthenticated);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("currentUser", currentUser);
            return "book-detail";
        } catch (Exception e) {
            log.error("Error cargando detalle de libro {}: {}", id, e.getMessage());
            model.addAttribute("error", "No se pudo cargar el libro");
            return "error";
        }
    }

    private ContentStats calculateStats(List<Review> reviews) {
        if (reviews.isEmpty()) return new ContentStats(0, 0.0, new int[5]);
        double total = 0;
        int[] dist = new int[5];
        for (Review r : reviews) {
            total += r.getStars();
            dist[r.getStars() - 1]++;
        }
        return new ContentStats(reviews.size(), total / reviews.size(), dist);
    }

}
