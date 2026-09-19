package cl.duoc.bancoxyz.bff.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCuentaWebDto {
    private Long cuentaId;
    private String nombreTitular;
    private Integer edadTitular;
    private String tipoCuenta;
    private Long saldoContable;
    private Long lineaSobregiro;
    private Long saldoTotalDisponible;
    private Double tasaInteresAnual;
    private Double interesMensualEstimado;
    private String estadoCuenta;
    private Integer totalTransacciones;
    private List<TransaccionWebDto> historialTransacciones;
    private List<Map<String, Object>> historialAnual;
    private Map<String, Object> metadatosWeb;
}
