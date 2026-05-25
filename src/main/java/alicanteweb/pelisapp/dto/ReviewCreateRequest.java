package alicanteweb.pelisapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReviewCreateRequest {
    // userId is ignored by the controller (uses session authentication instead)
    private Long userId;

    @NotNull
    private Long movieId;

    @Size(max = 1000)
    private String text;

    @NotNull
    private Double stars;

}
