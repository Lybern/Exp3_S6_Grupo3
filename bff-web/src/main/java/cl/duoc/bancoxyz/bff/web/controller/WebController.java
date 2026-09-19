package cl.duoc.bancoxyz.bff.web.controller;

import cl.duoc.bancoxyz.bff.web.dto.DashboardWebDto;
import cl.duoc.bancoxyz.bff.web.dto.DetalleCuentaWebDto;
import cl.duoc.bancoxyz.bff.web.dto.TransaccionWebDto;
import cl.duoc.bancoxyz.bff.web.service.WebBffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/web")
@Tag(name = "BFF Web", description = "Endpoints para portal web (datos completos, historial y dashboard)")
public class WebController {

    private final WebBffService webBffService;

    public WebController(WebBffService webBffService) {
        this.webBffService = webBffService;
    }

    @Operation(summary = "Obtener detalle completo de cuenta para Web")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<DetalleCuentaWebDto> obtenerDetalle(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(webBffService.obtenerDetalleWeb(cuentaId));
    }

    @Operation(summary = "Listar transacciones históricas")
    @GetMapping("/cuentas/{cuentaId}/transacciones")
    public ResponseEntity<List<TransaccionWebDto>> listarTransacciones(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(webBffService.listarTodasTransaccionesWeb(cuentaId));
    }

    @Operation(summary = "Dashboard global analítico")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardWebDto> obtenerDashboard() {
        return ResponseEntity.ok(webBffService.obtenerDashboardWeb());
    }
}
