package cl.duoc.bancoxyz.bff.movil.service;

import cl.duoc.bancoxyz.bff.movil.client.CoreBancarioClient;
import cl.duoc.bancoxyz.bff.movil.dto.ResumenCuentaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.SolicitudTransferenciaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MovilBffService {

    private final CoreBancarioClient coreClient;

    public MovilBffService(CoreBancarioClient coreClient) {
        this.coreClient = coreClient;
    }

    public ResumenCuentaMovilDto obtenerResumenMovil(Long cuentaId) {
        Map<String, Object> c = coreClient.obtenerCuentaPorId(cuentaId);
        List<TransaccionMovilDto> todasTx = coreClient.obtenerTransaccionesPorCuenta(cuentaId);

        // Optimización móvil: solo Top 3
        List<TransaccionMovilDto> ultimas3 = todasTx.stream()
                .limit(3)
                .collect(Collectors.toList());

        long saldo = Long.parseLong(c.get("saldoContable").toString());
        long sobregiro = Long.parseLong(c.get("lineaSobregiro").toString());

        return new ResumenCuentaMovilDto(
                cuentaId,
                c.get("nombreTitular").toString(),
                c.get("tipoCuenta").toString(),
                saldo + sobregiro,
                ultimas3,
                "MOVIL"
        );
    }

    public Map<String, Object> obtenerSaldoRapido(Long cuentaId) {
        Map<String, Object> c = coreClient.obtenerCuentaPorId(cuentaId);
        long saldo = Long.parseLong(c.get("saldoContable").toString());
        long sobregiro = Long.parseLong(c.get("lineaSobregiro").toString());

        return Map.of(
                "cuentaId", cuentaId,
                "saldoDisponible", saldo + sobregiro,
                "canal", "MOVIL"
        );
    }

    public Map<String, String> procesarTransferencia(Long cuentaId, SolicitudTransferenciaMovilDto solicitud) {
        coreClient.ejecutarTransferencia(cuentaId, solicitud.getCuentaDestinoId(), solicitud.getMonto(), solicitud.getComentario());
        return Map.of(
                "estado", "EXITOSA",
                "mensaje", "Transferencia de $" + solicitud.getMonto() + " enviada a la cuenta " + solicitud.getCuentaDestinoId()
        );
    }
}
