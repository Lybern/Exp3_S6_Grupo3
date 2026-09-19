package cl.duoc.bancoxyz.bff.movil.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCuentaMovilDto {
    private Long cuentaId;
    private String nombreTitular;
    private String tipoCuenta;
    private Long saldoDisponible;
    private List<TransaccionMovilDto> ultimosMovimientos;
    private String canal;
}
