package alicanteweb.pelisapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;

    private String sharedContentType;
    private Long sharedContentId;
    private String sharedContentTitle;
    private String sharedContentPoster;
}