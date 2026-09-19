package cl.duoc.bancoxyz.bff.cajero.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    @NotBlank(message = "El identificador de operador/terminal es obligatorio")
    private String username;
    @NotBlank(message = "La contraseña del terminal es obligatoria")
    private String password;
}
