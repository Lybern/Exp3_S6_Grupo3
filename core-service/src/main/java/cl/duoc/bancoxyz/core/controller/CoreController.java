package cl.duoc.bancoxyz.core.controller;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.MovimientoAnual;
import cl.duoc.bancoxyz.core.model.Transaccion;
import cl.duoc.bancoxyz.core.service.BancoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core")
@Tag(name = "Core Bancario", description = "Endpoints internos protegidos por Service Token")
public class CoreController {

    private final BancoService bancoService;

    public CoreController(BancoService bancoService) {
        this.bancoService = bancoService;
    }

    @Operation(summary = "Obtener cuenta por ID")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(bancoService.obtenerCuentaPorId(cuentaId));
    }

    @Operation(summary = "Listar todas las cuentas")
    @GetMapping("/cuentas/todas")
    public ResponseEntity<List<Cuenta>> obtenerTodasLasCuentas() {
        return ResponseEntity.ok(bancoService.obtenerTodasLasCuentas());
    }

    @Operation(summary = "Listar transacciones de una cuenta")
    @GetMapping("/cuentas/{cuentaId}/transacciones")
    public ResponseEntity<List<Transaccion>> obtenerTransacciones(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(bancoService.obtenerTransaccionesPorCuenta(cuentaId));
    }

    @Operation(summary = "Listar movimientos anuales de una cuenta")
    @GetMapping("/cuentas/{cuentaId}/anuales")
    public ResponseEntity<List<MovimientoAnual>> obtenerMovimientosAnuales(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(bancoService.obtenerMovimientosAnualesPorCuenta(cuentaId));
    }

    @Operation(summary = "Ejecutar retiro")
    @PostMapping("/operaciones/retiro")
    public ResponseEntity<Cuenta> ejecutarRetiro(@RequestBody Map<String, Object> req) {
        Long cuentaId = Long.parseLong(req.get("cuentaId").toString());
        Long monto = Long.parseLong(req.get("monto").toString());
        String canal = req.getOrDefault("canal", "ATM").toString();
        return ResponseEntity.ok(bancoService.ejecutarRetiro(cuentaId, monto, canal));
    }

    @Operation(summary = "Ejecutar transferencia")
    @PostMapping("/operaciones/transferencia")
    public ResponseEntity<Map<String, String>> ejecutarTransferencia(@RequestBody Map<String, Object> req) {
        Long origen = Long.parseLong(req.get("cuentaOrigenId").toString());
        Long destino = Long.parseLong(req.get("cuentaDestinoId").toString());
        Long monto = Long.parseLong(req.get("monto").toString());
        String comentario = req.getOrDefault("comentario", "").toString();
        bancoService.ejecutarTransferencia(origen, destino, monto, comentario);
        return ResponseEntity.ok(Map.of("mensaje", "Transferencia procesada exitosamente"));
    }
}
