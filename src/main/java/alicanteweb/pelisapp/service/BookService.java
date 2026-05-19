package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.dto.BookDetailDTO;
import alicanteweb.pelisapp.dto.BookListDTO;
import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.service.image.SupabaseImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    /** Categorías amplias → palabras clave a buscar en el campo categories de Google Books */
    private static final Map<String, List<String>> BROAD_CATEGORIES = new LinkedHashMap<>();
    static {
        BROAD_CATEGORIES.put("Ficción",               List.of("Fiction", "Novel", "Literature", "Literary"));
        BROAD_CATEGORIES.put("Ciencia Ficción",        List.of("Science Fiction", "Fantasy", "Sci-Fi", "Dystopian"));
        BROAD_CATEGORIES.put("Thriller y Misterio",   List.of("Thriller", "Mystery", "Suspense", "Crime", "Detective"));
        BROAD_CATEGORIES.put("Terror",                 List.of("Horror", "Ghost", "Supernatural"));
        BROAD_CATEGORIES.put("Romance",                List.of("Romance", "Love Story"));
        BROAD_CATEGORIES.put("Historia y Biografía",  List.of("History", "Biography", "Historical", "Memoir", "True Story"));
        BROAD_CATEGORIES.put("Ciencia y Tecnología",  List.of("Science", "Technology", "Computing", "Mathematics", "Physics", "Computer"));
        BROAD_CATEGORIES.put("Infantil y Juvenil",     List.of("Juvenile", "Children", "Young Adult", "Kids"));
        BROAD_CATEGORIES.put("Autoayuda",              List.of("Self-Help", "Self Help", "Motivation", "Psychology", "Personal Development"));
        BROAD_CATEGORIES.put("Economía y Negocios",   List.of("Business", "Economics", "Finance", "Management", "Entrepreneurship"));
        BROAD_CATEGORIES.put("Arte y Cultura",         List.of("Art", "Music", "Film", "Photography", "Architecture", "Culture"));
        BROAD_CATEGORIES.put("Viajes y Aventura",      List.of("Travel", "Adventure", "Geography", "Exploration"));
        BROAD_CATEGORIES.put("Cocina",                 List.of("Cooking", "Food", "Gastronomy", "Cuisine", "Recipe"));
    }

    /** Devuelve el nombre en español de la categoría amplia a partir de cualquier keyword de Google Books */
    public static String toBroadCategory(String rawCategory) {
        if (rawCategory == null) return null;
        String lower = rawCategory.toLowerCase();
        for (var entry : BROAD_CATEGORIES.entrySet()) {
            if (entry.getValue().stream().anyMatch(kw -> lower.contains(kw.toLowerCase()))) {
                return entry.getKey();
            }
        }
        return null;
    }

    private final BookRepository bookRepository;
    private final SupabaseImageStorage supabaseImageStorage;

    public Page<BookListDTO> getAllBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findAll(pageable);
        List<BookListDTO> dtos = mapWithRatingStats(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public Optional<BookDetailDTO> getBookById(Long id) {
        return bookRepository.findById(id).map(this::toDetailDTO);
    }

    public List<BookListDTO> searchBooks(String query) {
        return mapWithRatingStats(bookRepository.findByTitleContainingIgnoreCase(query));
    }

    public Page<BookListDTO> getBooksByCategory(String category, Pageable pageable) {
        List<String> keywords = BROAD_CATEGORIES.get(category);
        if (keywords == null) {
            // Fallback: búsqueda directa por si acaso
            Page<Book> page = bookRepository.findByCategoriesContainingIgnoreCase(category, pageable);
            return new PageImpl<>(mapWithRatingStats(page.getContent()), pageable, page.getTotalElements());
        }
        // Construir patrón regex para PostgreSQL: keyword1|keyword2|...
        String pattern = keywords.stream()
                .map(k -> k.replace(" ", "[ ]?"))  // "Self Help" y "Self-Help"
                .reduce((a, b) -> a + "|" + b)
                .orElse(keywords.get(0));
        Page<Book> page = bookRepository.findByCategoriesMatchingPattern(pattern, pageable);
        return new PageImpl<>(mapWithRatingStats(page.getContent()), pageable, page.getTotalElements());
    }

    /** Devuelve las categorías amplias que tienen al menos un libro en el catálogo */
    public List<String> getAvailableCategories() {
        List<String> rawCategories = bookRepository.findAllCategoryStrings().stream()
                .flatMap(c -> Arrays.stream(c.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        return BROAD_CATEGORIES.keySet().stream()
                .filter(broad -> {
                    List<String> keywords = BROAD_CATEGORIES.get(broad);
                    return rawCategories.stream().anyMatch(raw ->
                            keywords.stream().anyMatch(kw -> raw.toLowerCase().contains(kw.toLowerCase())));
                })
                .toList();
    }

    private List<BookListDTO> mapWithRatingStats(List<Book> books) {
        if (books.isEmpty()) return List.of();
        List<Long> ids = books.stream().map(Book::getId).toList();
        Map<Long, double[]> ratingMap = RatingStatsHelper.buildRatingMap(bookRepository.findRatingStatsByIds(ids));
        return books.stream().map(b -> toListDTO(b, ratingMap.get(b.getId()))).toList();
    }

    private BookListDTO toListDTO(Book book, double[] stats) {
        BookListDTO dto = new BookListDTO();
        dto.setId(book.getId());
        dto.setGoogleBooksId(book.getGoogleBooksId());
        dto.setTitle(book.getTitle());
        dto.setAuthors(book.getAuthors());
        dto.setPublisher(book.getPublisher());
        dto.setPublishedDate(book.getPublishedDate());
        dto.setCategories(book.getCategories());
        dto.setCoverUrl(displayCoverUrl(book.getCoverUrl()));
        if (stats != null && stats[1] > 0) {
            dto.setReviewCount((int) stats[1]);
            dto.setAvgRating(stats[0]);
        } else {
            dto.setReviewCount(0);
        }
        return dto;
    }

    private BookDetailDTO toDetailDTO(Book book) {
        BookDetailDTO dto = new BookDetailDTO();
        dto.setId(book.getId());
        dto.setGoogleBooksId(book.getGoogleBooksId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setAuthors(book.getAuthors());
        dto.setPublisher(book.getPublisher());
        dto.setPublishedDate(book.getPublishedDate());
        dto.setDescription(book.getDescription());
        dto.setPageCount(book.getPageCount());
        dto.setCategories(book.getCategories());
        dto.setLanguage(book.getLanguage());
        dto.setCoverUrl(displayCoverUrl(book.getCoverUrl()));
        return dto;
    }
    private String displayCoverUrl(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (supabaseImageStorage.wantsSupabase() && !supabaseImageStorage.isSupabasePublicUrl(coverUrl)) {
            return null;
        }
        return coverUrl;
    }
}
