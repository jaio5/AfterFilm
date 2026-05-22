package alicanteweb.pelisapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;

@Service
@Slf4j
public class GoogleBooksClient {

    private static final String GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1";
    private final WebClient webClient;
    private final String apiKey;

    @Value("${app.google-books.lang-restrict:}")
    private String langRestrict;

    public GoogleBooksClient(@Value("${app.google-books.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(GOOGLE_BOOKS_BASE_URL)
                .build();
    }

    public JsonNode searchBooks(String query, int startIndex) {
        return searchBooks(query, startIndex, 40);
    }

    public JsonNode searchBooks(String query, int startIndex, int maxResults) {
        log.debug("Searching Google Books for: {} (startIndex={})", query, startIndex);
        try {
            return webClient.get()
                    .uri(uriBuilder -> buildSearchUri(uriBuilder, query, startIndex, maxResults, true))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(15));
        } catch (WebClientResponseException we) {
            log.warn("Google Books searchBooks failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
            return retrySearchWithoutKey(query, startIndex, maxResults);
        } catch (Exception e) {
            log.warn("Google Books searchBooks failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode getBookDetail(String volumeId) {
        log.debug("Getting Google Books volume: {}", volumeId);
        try {
            return webClient.get()
                    .uri(uriBuilder -> buildDetailUri(uriBuilder, volumeId, true))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(15));
        } catch (WebClientResponseException we) {
            log.warn("Google Books getBookDetail failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
            return retryDetailWithoutKey(volumeId);
        } catch (Exception e) {
            log.warn("Google Books getBookDetail failed: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode retrySearchWithoutKey(String query, int startIndex, int maxResults) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            log.info("Reintentando búsqueda de Google Books sin API key");
            return webClient.get()
                    .uri(uriBuilder -> buildSearchUri(uriBuilder, query, startIndex, maxResults, false))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(15));
        } catch (Exception e) {
            log.warn("Google Books unauthenticated searchBooks failed: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode retryDetailWithoutKey(String volumeId) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            log.info("Reintentando detalle de Google Books sin API key para {}", volumeId);
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/volumes/{volumeId}").build(volumeId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(15));
        } catch (Exception e) {
            log.warn("Google Books unauthenticated getBookDetail failed: {}", e.getMessage());
            return null;
        }
    }

    private URI buildSearchUri(UriBuilder uriBuilder, String query, int startIndex, int maxResults, boolean includeKey) {
        uriBuilder.path("/volumes")
                .queryParam("q", query)
                .queryParam("startIndex", startIndex)
                .queryParam("maxResults", Math.min(Math.max(1, maxResults), 40));
        if (includeKey) {
            optionalApiKey().ifPresent(key -> uriBuilder.queryParam("key", key));
        }
        if (langRestrict != null && !langRestrict.isBlank()) {
            uriBuilder.queryParam("langRestrict", langRestrict);
        }
        return uriBuilder.build();
    }

    private URI buildDetailUri(UriBuilder uriBuilder, String volumeId, boolean includeKey) {
        uriBuilder.path("/volumes/{volumeId}");
        if (includeKey) {
            optionalApiKey().ifPresent(key -> uriBuilder.queryParam("key", key));
        }
        return uriBuilder.build(volumeId);
    }

    private java.util.Optional<String> optionalApiKey() {
        return apiKey == null || apiKey.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(apiKey);
    }
}
