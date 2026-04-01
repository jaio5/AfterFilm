package alicanteweb.pelisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TvShowListDTO {
    private Long id;
    private Long tmdbId;
    private String title;
    private String overview;
    private String posterPath;
    private String posterLocalPath;
    private LocalDate firstAirDate;
    private Integer numberOfSeasons;
    private String genres;
    private String status;
    private Double avgRating;
    private Integer reviewCount;
}
