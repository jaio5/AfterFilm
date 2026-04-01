package alicanteweb.pelisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailDTO {
    private Long id;
    private String googleBooksId;
    private String isbn;
    private String title;
    private String authors;
    private String publisher;
    private String publishedDate;
    private String description;
    private Integer pageCount;
    private String categories;
    private String language;
    private String coverUrl;
}
