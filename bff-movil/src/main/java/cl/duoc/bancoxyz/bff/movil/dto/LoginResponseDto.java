package cl.duoc.bancoxyz.bff.movil.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    @Builder.Default
    private String tipoToken = "Bearer";
    private String username;
    private String canal;
    private String rol;
    private long expiracionMilisegundos;
}
