package alicanteweb.pelisapp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ReviewReplyDTO {
    private Long id;
    private Long reviewId;
    private SimpleUserDTO user;
    private String text;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class SimpleUserDTO {
        private Long id;
        private String username;
    }
}
