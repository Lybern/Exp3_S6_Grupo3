package cl.duoc.bancoxyz.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {
    private Long cuentaId;
    private String nombreTitular;
    private Long saldoContable;
    private Integer edadTitular;
    private String tipoCuenta;
    private Long lineaSobregiro;
    private Double tasaInteresAnual;
    private String estado;
}
