package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.repository.BookRepository;
import alicanteweb.pelisapp.service.image.SupabaseImageStorage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleBooksLoaderService {

    private static final List<String> FALLBACK_ANY_STYLE_QUERIES = List.of(
            "subject:fiction", "subject:science fiction", "subject:fantasy", "subject:history",
            "subject:biography", "subject:mystery", "subject:romance", "subject:thriller",
            "subject:philosophy", "subject:technology", "subject:art", "subject:business",
            "subject:cooking", "subject:travel", "subject:poetry", "subject:comics",
            "subject:juvenile fiction", "subject:self-help", "novela", "historia",
            "ciencia", "poesia", "teatro", "ensayo", "aventura", "infantil",
            "a", "the", "of", "life", "world", "love", "story", "intitle:a", "intitle:e", "inauthor:a"
    );

    private final GoogleBooksClient googleBooksClient;
    private final BookRepository bookRepository;
    private final ImageService imageService;
    private final SupabaseImageStorage supabaseImageStorage;

    @Transactional
    public Book importOrUpdateByGoogleId(String googleId) {
        JsonNode details = googleBooksClient.getBookDetail(googleId);
        if (details == null || details.isNull()) {
            log.warn("Google Books returned no details for id {}", googleId);
            return null;
        }
        Optional<Book> existing = bookRepository.findByGoogleBooksId(googleId);
        if (existing.isPresent()) {
            Book book = existing.get();
            mergeFromGoogleBooks(book, details);
            return bookRepository.save(book);
        }
        Book book = new Book();
        book.setGoogleBooksId(googleId);
        mergeFromGoogleBooks(book, details);
        try {
            return bookRepository.save(book);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent insert for googleId {}", googleId);
            return bookRepository.findByGoogleBooksId(googleId).orElseThrow(() -> ex);
        }
    }

    @Transactional
    public Book importOrUpdateFromSearchItem(JsonNode item) {
        if (item == null || item.isNull()) {
            return null;
        }
        String googleId = item.path("id").asText(null);
        if (googleId == null || googleId.isBlank()) {
            return null;
        }

        Optional<Book> existing = bookRepository.findByGoogleBooksId(googleId);
        Book book = existing.orElseGet(Book::new);
        book.setGoogleBooksId(googleId);
        mergeFromGoogleBooks(book, item);

        try {
            return bookRepository.save(book);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent insert for googleId {}", googleId);
            return bookRepository.findByGoogleBooksId(googleId).orElseThrow(() -> ex);
        }
    }

    public int searchAndImport(String query, int maxResults) {
        log.info("Iniciando importación de libros — query: '{}', máximo: {}", query, maxResults);
        int imported = importNewBooksFromQuery(query, maxResults);
        if (imported < maxResults) {
            log.info("La búsqueda '{}' no completó el objetivo. Buscando libros nuevos en otros estilos...", query);
            for (String fallbackQuery : buildFallbackQueries(query)) {
                if (imported >= maxResults) {
                    break;
                }
                if (fallbackQuery.equalsIgnoreCase(query)) {
                    continue;
                }
                imported += importNewBooksFromQuery(fallbackQuery, maxResults - imported);
            }
        }
        log.info("Importación completada — query: '{}', total importados: {}", query, imported);
        return imported;
    }

    private int importNewBooksFromQuery(String query, int maxResults) {
        int imported = 0;
        int startIndex = 0;
        int pagesWithoutNewBooks = 0;
        while (imported < maxResults && startIndex < 1000) {
            JsonNode resp = googleBooksClient.searchBooks(query, startIndex, 40);
            if (resp == null || !resp.has("items")) break;
            int importedBeforePage = imported;
            for (JsonNode item : resp.path("items")) {
                if (imported >= maxResults) break;
                String id = item.path("id").asText(null);
                if (id == null || id.isBlank()) continue;
                try {
                    if (bookRepository.findByGoogleBooksId(id).isPresent()) {
                        continue;
                    }
                    Book book = importOrUpdateFromSearchItem(item);
                    if (book == null) {
                        book = importOrUpdateByGoogleId(id);
                    }
                    if (book != null) {
                        imported++;
                        if (imported % 10 == 0) {
                            log.info("  -> '{}': {} libros importados hasta ahora...", query, imported);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error importing book googleId={}: {}", id, e.getMessage());
                }
            }
            if (imported == importedBeforePage) {
                pagesWithoutNewBooks++;
                if (pagesWithoutNewBooks >= 5) {
                    break;
                }
            } else {
                pagesWithoutNewBooks = 0;
            }
            int totalItems = resp.path("totalItems").asInt(0);
            startIndex += 40;
            if (startIndex >= totalItems) break;
        }
        return imported;
    }

    private List<String> buildFallbackQueries(String originalQuery) {
        LinkedHashSet<String> queries = new LinkedHashSet<>(FALLBACK_ANY_STYLE_QUERIES);
        for (char ch = 'a'; ch <= 'z'; ch++) {
            queries.add("intitle:" + ch);
        }
        for (char ch = 'a'; ch <= 'z'; ch++) {
            queries.add("inauthor:" + ch);
        }
        queries.remove(originalQuery);
        return new ArrayList<>(queries);
    }

    @Transactional
    public boolean repairBookMetadataAndCover(Book book, boolean forceCover) {
        if (book == null || book.getGoogleBooksId() == null || book.getGoogleBooksId().isBlank()) {
            return false;
        }

        JsonNode details = googleBooksClient.getBookDetail(book.getGoogleBooksId());
        if (details == null || details.isNull()) {
            return false;
        }

        String previousAuthors = book.getAuthors();
        String previousCover = book.getCoverUrl();
        mergeFromGoogleBooks(book, details, forceCover);
        bookRepository.save(book);
        return !Objects.equals(previousAuthors, book.getAuthors())
                || !Objects.equals(previousCover, book.getCoverUrl());
    }

    private void mergeFromGoogleBooks(Book book, JsonNode volume) {
        mergeFromGoogleBooks(book, volume, false);
    }

    private void mergeFromGoogleBooks(Book book, JsonNode volume, boolean forceCover) {
        JsonNode info = volume.path("volumeInfo");
        if (info.isMissingNode()) return;

        if (book.getTitle() == null || book.getTitle().isBlank()) {
            String t = trimToNull(info.path("title").asText(null), 500);
            book.setTitle(t != null ? t : "Sin titulo");
        }
        if (book.getAuthors() == null || book.getAuthors().isBlank()) {
            if (info.has("authors")) {
                book.setAuthors(joinTextArray(info.path("authors"), 1000));
            }
        }
        if (book.getPublisher() == null) {
            book.setPublisher(trimToNull(info.path("publisher").asText(null), 500));
        }
        if (book.getPublishedDate() == null) {
            book.setPublishedDate(trimToNull(info.path("publishedDate").asText(null), 64));
        }
        if (book.getDescription() == null || book.getDescription().isBlank()) {
            book.setDescription(trimToNull(info.path("description").asText(null), 3000));
        }
        if (book.getPageCount() == null && info.hasNonNull("pageCount")) {
            book.setPageCount(info.path("pageCount").asInt());
        }
        if (book.getLanguage() == null) {
            book.setLanguage(trimToNull(info.path("language").asText(null), 32));
        }
        if (book.getCategories() == null && info.has("categories")) {
            book.setCategories(joinTextArray(info.path("categories"), 1000));
        }
        if (book.getIsbn() == null && info.has("industryIdentifiers")) {
            for (JsonNode ident : info.path("industryIdentifiers")) {
                String type = ident.path("type").asText("");
                if ("ISBN_13".equals(type) || "ISBN_10".equals(type)) {
                    book.setIsbn(trimToNull(ident.path("identifier").asText(null), 64));
                    if ("ISBN_13".equals(type)) break;
                }
            }
        }

        if (forceCover || shouldLoadCover(book.getCoverUrl())) {
            String coverUrl = bestCoverUrl(info.path("imageLinks"));
            if (coverUrl != null) {
                String prefix = book.getGoogleBooksId() == null || book.getGoogleBooksId().isBlank()
                        ? "book"
                        : "book_" + book.getGoogleBooksId();
                try {
                    String stored = imageService.downloadAndSave(coverUrl, prefix, "books");
                    if (stored != null) {
                        book.setCoverUrl(stored);
                    } else if (!supabaseImageStorage.wantsSupabase() && (book.getCoverUrl() == null || book.getCoverUrl().isBlank())) {
                        book.setCoverUrl(coverUrl);
                    }
                } catch (Exception e) {
                    log.warn("No se pudo descargar portada para libro googleId={}: {}", book.getGoogleBooksId(), e.getMessage());
                    if (!supabaseImageStorage.wantsSupabase() && (book.getCoverUrl() == null || book.getCoverUrl().isBlank())) {
                        book.setCoverUrl(coverUrl);
                    }
                }
            }
        }
    }

    private boolean shouldLoadCover(String currentCoverUrl) {
        if (currentCoverUrl == null || currentCoverUrl.isBlank()) {
            return true;
        }
        return supabaseImageStorage.wantsSupabase() && !supabaseImageStorage.isSupabasePublicUrl(currentCoverUrl);
    }

    private String bestCoverUrl(JsonNode imageLinks) {
        if (imageLinks == null || imageLinks.isMissingNode()) {
            return null;
        }
        return Stream.of("extraLarge", "large", "medium", "thumbnail", "smallThumbnail")
                .map(size -> imageLinks.path(size).asText(null))
                .filter(Objects::nonNull)
                .findFirst()
                .map(url -> url.replace("http://", "https://")
                        .replace("zoom=1", "zoom=0")
                        .replace("&edge=curl", ""))
                .orElse(null);
    }

    private String joinTextArray(JsonNode arrayNode, int maxLength) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : arrayNode) {
            String value = trimToNull(node.asText(null), maxLength);
            if (value == null) {
                continue;
            }
            String chunk = sb.length() == 0 ? value : ", " + value;
            if (sb.length() + chunk.length() > maxLength) {
                int remaining = maxLength - sb.length();
                if (remaining > 0) {
                    sb.append(chunk, 0, Math.min(remaining, chunk.length()));
                }
                break;
            }
            sb.append(chunk);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
