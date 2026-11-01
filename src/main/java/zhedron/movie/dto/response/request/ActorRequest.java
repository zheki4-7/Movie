package zhedron.movie.dto.response.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import zhedron.movie.enums.Gender;

@Data
public class ActorRequest {
    @NotNull(message = "Enter a name")
    @NotBlank(message = "Name must not be empty")
    private String name;

    @NotNull(message = "Enter a surname")
    @NotBlank(message = "Surname must not be empty")
    private String surname;

    @NotNull(message = "Enter a year")
    private Integer year;

    @NotNull(message = "Enter a your gender")
    private Gender gender;
}
