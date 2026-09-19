package cl.duoc.bancoxyz.bff.movil.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudTransferenciaMovilDto {
    @NotNull(message = "El ID de la cuenta de destino es obligatorio")
    private Long cuentaDestinoId;

    @NotNull(message = "El monto a transferir es obligatorio")
    @Min(value = 1, message = "El monto mínimo de transferencia debe ser de al menos $1")
    private Long monto;

    private String comentario;
}
