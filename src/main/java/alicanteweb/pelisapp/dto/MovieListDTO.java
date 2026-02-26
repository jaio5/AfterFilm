package alicanteweb.pelisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO simplificado para listar películas en la API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieListDTO {
    private Long id;
    private String title;
    private String description;
    private String posterPath;
    private String posterLocalPath;
    private LocalDate releaseDate;
    private Integer runtimeMinutes;
    private Long tmdbId;
    private List<String> categories;
    private Double avgRating;
    private Integer reviewCount;
}
