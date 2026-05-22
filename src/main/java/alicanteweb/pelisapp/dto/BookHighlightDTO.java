package alicanteweb.pelisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookHighlightDTO {
    private Long id;
    private String title;
    private String authors;
    private String publisher;
    private String publishedDate;
    private String description;
    private String categories;
    private String coverUrl;
    private Double avgRating;
    private Integer reviewCount;
}