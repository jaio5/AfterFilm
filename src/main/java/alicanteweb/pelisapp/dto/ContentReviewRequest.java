package alicanteweb.pelisapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentReviewRequest {

    @Size(max = 1000)
    private String text;

    @Min(1)
    @Max(5)
    private int stars;
}
