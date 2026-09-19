package cl.duoc.bancoxyz.bff.movil.controller;

import cl.duoc.bancoxyz.bff.movil.dto.ResumenCuentaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.SolicitudTransferenciaMovilDto;
import cl.duoc.bancoxyz.bff.movil.service.MovilBffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/movil")
@Tag(name = "BFF Móvil", description = "Endpoints optimizados para smartphones (respuestas ligeras)")
public class MovilController {

    private final MovilBffService movilBffService;

    public MovilController(MovilBffService movilBffService) {
        this.movilBffService = movilBffService;
    }

    @Operation(summary = "Resumen ligero de cuenta (Top 3 movimientos)")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<ResumenCuentaMovilDto> obtenerResumen(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(movilBffService.obtenerResumenMovil(cuentaId));
    }

    @Operation(summary = "Consulta rápida de saldo")
    @GetMapping("/cuentas/{cuentaId}/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(movilBffService.obtenerSaldoRapido(cuentaId));
    }

    @Operation(summary = "Transferencia rápida con Bean Validation")
    @PostMapping("/cuentas/{cuentaId}/transferencia")
    public ResponseEntity<Map<String, String>> transferir(
            @PathVariable Long cuentaId,
            @Valid @RequestBody SolicitudTransferenciaMovilDto solicitud) {
        return ResponseEntity.ok(movilBffService.procesarTransferencia(cuentaId, solicitud));
    }
}
