package cl.duoc.bancoxyz.bff.cajero.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaRetiroDto {
    private Long cuentaId;
    private Long montoRetirado;
    private Long saldoRestante;
    private String estadoTransaccion;
    private String terminalId;
    private LocalDateTime fechaHora;
}
