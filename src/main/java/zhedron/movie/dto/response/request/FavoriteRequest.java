package zhedron.movie.dto.response.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteRequest {
    @NotBlank(message = "Name must not be empty")
    @NotNull(message = "Enter a name")
    private String name;
}
