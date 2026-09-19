package cl.duoc.bancoxyz.bff.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWebDto {
    private Integer totalCuentasActivas;
    private Long capitalTotalCustodia;
    private Double saldoPromedioCuentas;
    private Map<String, Long> distribucionPorTipoCuenta;
    private Map<String, Object> metricasRiesgo;
    private String fechaGeneracion;
}
