package alicanteweb.pelisapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MovieDetailsDTO {
    private Long id;
    private Long tmdbId;
    private String title;
    private String overview;
    private String posterPath;
    private String releaseDate;
    private List<String> cast;
    private List<CastDTO> castMembers;
    private List<CrewDTO> directors;
    private List<CommentDTO> comments;

    @Setter
    @Getter
    public static class CommentDTO {
        private Long id;
        private String author;
        private String text;
        private Integer rating;

        public CommentDTO() {}

        public CommentDTO(Long id, String author, String text, Integer rating) {
            this.id = id;
            this.author = author;
            this.text = text;
            this.rating = rating;
        }

    }

}
