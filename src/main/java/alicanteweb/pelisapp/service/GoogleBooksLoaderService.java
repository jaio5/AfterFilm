package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.Book;
import alicanteweb.pelisapp.repository.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleBooksLoaderService {

    private final GoogleBooksClient googleBooksClient;
    private final BookRepository bookRepository;

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

    public int searchAndImport(String query, int maxResults) {
        int imported = 0;
        int startIndex = 0;
        log.info("Iniciando importación de libros — query: '{}', máximo: {}", query, maxResults);
        while (imported < maxResults) {
            JsonNode resp = googleBooksClient.searchBooks(query, startIndex);
            if (resp == null || !resp.has("items")) break;
            for (JsonNode item : resp.path("items")) {
                if (imported >= maxResults) break;
                String id = item.path("id").asText(null);
                if (id == null) continue;
                try {
                    Book book = importOrUpdateByGoogleId(id);
                    if (book != null) {
                        imported++;
                        if (imported % 10 == 0) {
                            log.info("  → '{}': {} libros importados hasta ahora...", query, imported);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error importing book googleId={}: {}", id, e.getMessage());
                }
            }
            int totalItems = resp.path("totalItems").asInt(0);
            startIndex += 40;
            if (startIndex >= totalItems) break;
        }
        log.info("Importación completada — query: '{}', total importados: {}", query, imported);
        return imported;
    }

    private void mergeFromGoogleBooks(Book book, JsonNode volume) {
        JsonNode info = volume.path("volumeInfo");
        if (info.isMissingNode()) return;

        if (book.getTitle() == null || book.getTitle().isBlank()) {
            String t = info.path("title").asText(null);
            if (t != null) book.setTitle(t);
        }
        if (book.getAuthors() == null) {
            if (info.has("authors")) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode a : info.path("authors")) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(a.asText());
                }
                if (sb.length() > 0) book.setAuthors(sb.toString());
            }
        }
        if (book.getPublisher() == null) {
            book.setPublisher(info.path("publisher").asText(null));
        }
        if (book.getPublishedDate() == null) {
            book.setPublishedDate(info.path("publishedDate").asText(null));
        }
        if (book.getDescription() == null || book.getDescription().isBlank()) {
            book.setDescription(info.path("description").asText(null));
        }
        if (book.getPageCount() == null && info.hasNonNull("pageCount")) {
            book.setPageCount(info.path("pageCount").asInt());
        }
        if (book.getLanguage() == null) {
            book.setLanguage(info.path("language").asText(null));
        }
        if (book.getCategories() == null && info.has("categories")) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode c : info.path("categories")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.asText());
            }
            if (sb.length() > 0) book.setCategories(sb.toString());
        }
        // ISBN
        if (book.getIsbn() == null && info.has("industryIdentifiers")) {
            for (JsonNode ident : info.path("industryIdentifiers")) {
                String type = ident.path("type").asText("");
                if ("ISBN_13".equals(type) || "ISBN_10".equals(type)) {
                    book.setIsbn(ident.path("identifier").asText(null));
                    if ("ISBN_13".equals(type)) break;
                }
            }
        }
        // Cover URL from Google Books CDN — prefer highest resolution available
        if (book.getCoverUrl() == null) {
            JsonNode imgs = info.path("imageLinks");
            if (!imgs.isMissingNode()) {
                Stream.of("extraLarge", "large", "medium", "thumbnail", "smallThumbnail")
                    .map(size -> imgs.path(size).asText(null))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(url -> url.replace("http://", "https://")
                                   .replace("zoom=1", "zoom=0")
                                   .replace("&edge=curl", ""))
                    .ifPresent(book::setCoverUrl);
            }
        }
    }
}
