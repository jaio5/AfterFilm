package alicanteweb.pelisapp.controller;

import java.util.Locale;
import java.util.Set;

public final class EndpointSanitizer {

    private static final int DEFAULT_TEXT_MAX = 120;
    private static final int DEFAULT_PAGE_MAX = 10_000;
    private static final Set<String> CONTENT_TYPES = Set.of("movie", "movies", "series", "tv", "book", "books");

    private EndpointSanitizer() {
    }

    public static int page(int page) {
        if (page < 0) {
            return 0;
        }
        return Math.min(page, DEFAULT_PAGE_MAX);
    }

    public static int size(int size, int defaultSize, int maxSize) {
        if (size < 1) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }

    public static Long id(Long id, String fieldName) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException(fieldName + " no válido");
        }
        return id;
    }

    public static String requiredText(String value, String fieldName) {
        return requiredText(value, fieldName, DEFAULT_TEXT_MAX);
    }

    public static String requiredText(String value, String fieldName, int maxLength) {
        String clean = optionalText(value, maxLength);
        if (clean == null) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }
        return clean;
    }

    public static String optionalText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String clean = value.strip().replaceAll("\\p{Cntrl}", "");
        if (clean.isBlank()) {
            return null;
        }
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    public static String username(String username) {
        String clean = requiredText(username, "username", 50);
        if (!clean.matches("[A-Za-z0-9_.-]{1,50}")) {
            throw new IllegalArgumentException("username no válido");
        }
        return clean;
    }

    public static String contentType(String contentType) {
        String clean = requiredText(contentType, "contentType", 20).toLowerCase(Locale.ROOT);
        if (!CONTENT_TYPES.contains(clean)) {
            throw new IllegalArgumentException("contentType no válido");
        }
        return switch (clean) {
            case "movies" -> "movie";
            case "tv" -> "series";
            case "books" -> "book";
            default -> clean;
        };
    }

    public static String listType(String listType) {
        String clean = requiredText(listType, "listType", 20).toUpperCase(Locale.ROOT);
        if (!"FAVORITE".equals(clean) && !"WATCHLIST".equals(clean)) {
            throw new IllegalArgumentException("listType debe ser FAVORITE o WATCHLIST");
        }
        return clean;
    }
}
