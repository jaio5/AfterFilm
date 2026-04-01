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
        log.debug("Searching Google Books for: {} (startIndex={})", query, startIndex);
        try {
            return webClient.get()
                    .uri(uriBuilder -> buildSearchUri(uriBuilder, query, startIndex))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(15));
        } catch (WebClientResponseException we) {
            log.warn("Google Books searchBooks failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Google Books searchBooks failed: {}", e.getMessage());
        }
        return null;
    }

    public JsonNode getBookDetail(String volumeId) {
        log.debug("Getting Google Books volume: {}", volumeId);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes/{volumeId}")
                            .queryParam("key", apiKey)
                            .build(volumeId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(15));
        } catch (WebClientResponseException we) {
            log.warn("Google Books getBookDetail failed: status={} body={}", we.getStatusCode().value(), we.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Google Books getBookDetail failed: {}", e.getMessage());
        }
        return null;
    }

    private URI buildSearchUri(UriBuilder uriBuilder, String query, int startIndex) {
        uriBuilder.path("/volumes")
                .queryParam("q", query)
                .queryParam("startIndex", startIndex)
                .queryParam("maxResults", 40)
                .queryParam("key", apiKey);
        if (langRestrict != null && !langRestrict.isBlank()) {
            uriBuilder.queryParam("langRestrict", langRestrict);
        }
        return uriBuilder.build();
    }
}