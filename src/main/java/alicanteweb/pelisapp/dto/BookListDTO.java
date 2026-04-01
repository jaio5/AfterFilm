package alicanteweb.pelisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookListDTO {
    private Long id;
    private String googleBooksId;
    private String title;
    private String authors;
    private String publisher;
    private String publishedDate;
    private String categories;
    private String coverUrl;
    private Double avgRating;
    private Integer reviewCount;
}
