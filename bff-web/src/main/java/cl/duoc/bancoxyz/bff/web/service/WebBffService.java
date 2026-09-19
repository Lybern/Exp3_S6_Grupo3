package cl.duoc.bancoxyz.bff.web.service;

import cl.duoc.bancoxyz.bff.web.client.CoreBancarioClient;
import cl.duoc.bancoxyz.bff.web.dto.DashboardWebDto;
import cl.duoc.bancoxyz.bff.web.dto.DetalleCuentaWebDto;
import cl.duoc.bancoxyz.bff.web.dto.TransaccionWebDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WebBffService {

    private final CoreBancarioClient coreClient;

    public WebBffService(CoreBancarioClient coreClient) {
        this.coreClient = coreClient;
    }

    public DetalleCuentaWebDto obtenerDetalleWeb(Long cuentaId) {
        Map<String, Object> c = coreClient.obtenerCuentaPorId(cuentaId);
        List<TransaccionWebDto> txs = coreClient.obtenerTransaccionesPorCuenta(cuentaId);
        List<Map<String, Object>> anuales = coreClient.obtenerMovimientosAnualesPorCuenta(cuentaId);

        long saldo = Long.parseLong(c.get("saldoContable").toString());
        long sobregiro = Long.parseLong(c.get("lineaSobregiro").toString());
        double tasa = Double.parseDouble(c.get("tasaInteresAnual").toString());
        double interesMensual = (saldo * (tasa / 100.0)) / 12.0;

        return new DetalleCuentaWebDto(
                cuentaId,
                c.get("nombreTitular").toString(),
                Integer.parseInt(c.get("edadTitular").toString()),
                c.get("tipoCuenta").toString(),
                saldo,
                sobregiro,
                saldo + sobregiro,
                tasa,
                Math.round(interesMensual * 100.0) / 100.0,
                c.get("estado").toString(),
                txs.size(),
                txs,
                anuales,
                Map.of("canal", "WEB", "version", "2.0")
        );
    }

    public List<TransaccionWebDto> listarTodasTransaccionesWeb(Long cuentaId) {
        return coreClient.obtenerTransaccionesPorCuenta(cuentaId);
    }

    public DashboardWebDto obtenerDashboardWeb() {
        List<Map<String, Object>> cuentas = coreClient.obtenerTodasLasCuentas();

        int total = cuentas.size();
        long capital = cuentas.stream().mapToLong(c -> Long.parseLong(c.get("saldoContable").toString())).sum();
        double promedio = total > 0 ? (double) capital / total : 0.0;

        Map<String, Long> dist = cuentas.stream()
                .collect(Collectors.groupingBy(c -> c.get("tipoCuenta").toString(), Collectors.counting()));

        return new DashboardWebDto(
                total,
                capital,
                Math.round(promedio * 100.0) / 100.0,
                dist,
                Map.of("indicadorRiesgo", "BAJO", "coberturaLiquidez", "99.8%"),
                LocalDateTime.now().toString()
        );
    }
}
