package cl.duoc.bancoxyz.bff.cajero.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRetiroCajeroDto {

    @NotNull(message = "El monto a retirar es obligatorio")
    @Min(value = 5000, message = "El monto mínimo de retiro en cajero es de $5.000")
    private Long monto;

    @NotBlank(message = "El PIN de seguridad de la tarjeta es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe constar exactamente de 4 dígitos numéricos")
    private String pin;

    @NotBlank(message = "El identificador del terminal físico es obligatorio")
    private String terminalId;
}
