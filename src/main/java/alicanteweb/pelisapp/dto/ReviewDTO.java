package alicanteweb.pelisapp.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ReviewDTO {
    private Long id;
    private SimpleUserDTO user;
    private SimpleMovieDTO movie;
    private String text;
    private Double stars;
    private Instant createdAt;
    private Long likesCount;

    @Data
    public static class SimpleUserDTO {
        private Long id;
        private String username;
    }

    @Data
    public static class SimpleMovieDTO {
        private Long id;
        private String title;
    }
}
