package zhedron.movie.dto.response.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Password must not be empty")
    @NotNull(message = "Enter a password")
    private String password;
}
