package alicanteweb.pelisapp.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String username;
    private String displayName;
    private Integer criticLevel;
    private List<String> roles;

    public UserDTO() {}

    public UserDTO(Long id, String username, String displayName, Integer criticLevel, List<String> roles) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.criticLevel = criticLevel;
        this.roles = roles;
    }

    // Constructor antiguo para compatibilidad
    public UserDTO(Long id, String username, String displayName, Integer criticLevel) {
        this(id, username, displayName, criticLevel, null);
    }
}
