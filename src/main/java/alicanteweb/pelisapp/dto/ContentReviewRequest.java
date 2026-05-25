package alicanteweb.pelisapp.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentReviewRequest {

    @Size(max = 1000)
    private String text;

    private Double stars;
}
