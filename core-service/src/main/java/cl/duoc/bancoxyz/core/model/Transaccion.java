package cl.duoc.bancoxyz.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {
    private Long id;
    private Long cuentaId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    private Long monto;
    private String tipo;
    private String descripcion;
    private String canalOrigen;
}
