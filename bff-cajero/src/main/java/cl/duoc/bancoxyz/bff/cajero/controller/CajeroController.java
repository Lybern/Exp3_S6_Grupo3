package cl.duoc.bancoxyz.bff.cajero.controller;

import cl.duoc.bancoxyz.bff.cajero.dto.ConsultaSaldoCajeroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.RespuestaRetiroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.SolicitudRetiroCajeroDto;
import cl.duoc.bancoxyz.bff.cajero.service.CajeroBffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cajero")
@Tag(name = "BFF Cajero Automático", description = "Operaciones críticas para terminales físicos ATM")
public class CajeroController {

    private final CajeroBffService cajeroBffService;

    public CajeroController(CajeroBffService cajeroBffService) {
        this.cajeroBffService = cajeroBffService;
    }

    @Operation(summary = "Consultar saldo disponible en cajero")
    @GetMapping("/cuentas/{cuentaId}/saldo")
    public ResponseEntity<ConsultaSaldoCajeroDto> consultarSaldo(
            @PathVariable Long cuentaId,
            @RequestParam(required = false, defaultValue = "ATM-TERMINAL-01") String terminalId) {
        return ResponseEntity.ok(cajeroBffService.consultarSaldoCajero(cuentaId, terminalId));
    }

    @Operation(summary = "Ejecutar retiro en efectivo (Bean Validation: PIN 4 dígitos, múltiplos $5.000)")
    @PostMapping("/cuentas/{cuentaId}/retiro")
    public ResponseEntity<RespuestaRetiroDto> retirar(
            @PathVariable Long cuentaId,
            @Valid @RequestBody SolicitudRetiroCajeroDto solicitud) {
        return ResponseEntity.ok(cajeroBffService.procesarRetiro(cuentaId, solicitud));
    }
}
