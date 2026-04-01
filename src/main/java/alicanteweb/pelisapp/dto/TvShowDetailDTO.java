package alicanteweb.pelisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TvShowDetailDTO {
    private Long id;
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private String posterPath;
    private String posterLocalPath;
    private String backdropPath;
    private LocalDate firstAirDate;
    private Integer numberOfSeasons;
    private Integer numberOfEpisodes;
    private String genres;
    private String status;
    private String language;
    private List<CastDTO> castMembers;
    private List<CrewDTO> directors;
}
