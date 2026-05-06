package alicanteweb.pelisapp.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for building a rating stats map from raw repository query results.
 * Shared by MovieService, TvShowService and BookService to avoid duplication.
 *
 * Row format: [id (Number), avgRating (Number|null), reviewCount (Number)]
 */
public final class RatingStatsHelper {

    private RatingStatsHelper() {}

    /**
     * Converts raw Object[] rows from a findRatingStatsByIds query into a map of
     * id → double[]{avgRating, reviewCount}.
     */
    public static Map<Long, double[]> buildRatingMap(List<Object[]> rows) {
        Map<Long, double[]> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 3 || row[0] == null || row[2] == null) continue;
            Long id = ((Number) row[0]).longValue();
            Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            long count = ((Number) row[2]).longValue();
            map.put(id, new double[]{avg != null ? avg : 0.0, count});
        }
        return map;
    }
}
