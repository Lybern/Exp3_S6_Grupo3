package cl.duoc.bancoxyz.bff.cajero.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaSaldoCajeroDto {
    private Long cuentaId;
    private String nombreTitular;
    private Long saldoDisponible;
    private Long limiteMaximoGiroDispensador;
    private String terminalId;
    private String estadoOperativo;
}
