package cl.duoc.bancoxyz.bff.cajero.service;

import cl.duoc.bancoxyz.bff.cajero.client.CoreBancarioClient;
import cl.duoc.bancoxyz.bff.cajero.dto.ConsultaSaldoCajeroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.RespuestaRetiroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.SolicitudRetiroCajeroDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CajeroBffService {

    private final CoreBancarioClient coreClient;

    @Value("${banco.cajero.limite-maximo-giro:200000}")
    private Long limiteMaximoGiro;

    @Value("${banco.cajero.multiplo-giro:5000}")
    private Long multiploGiro;

    public CajeroBffService(CoreBancarioClient coreClient) {
        this.coreClient = coreClient;
    }

    public ConsultaSaldoCajeroDto consultarSaldoCajero(Long cuentaId, String terminalId) {
        Map<String, Object> c = coreClient.obtenerCuentaPorId(cuentaId);
        long saldo = Long.parseLong(c.get("saldoContable").toString());
        long sobregiro = Long.parseLong(c.get("lineaSobregiro").toString());

        return new ConsultaSaldoCajeroDto(
                cuentaId,
                c.get("nombreTitular").toString(),
                saldo + sobregiro,
                limiteMaximoGiro,
                terminalId != null ? terminalId : "ATM-TERMINAL-GENERIC",
                "OPERATIVO"
        );
    }

    public RespuestaRetiroDto procesarRetiro(Long cuentaId, SolicitudRetiroCajeroDto solicitud) {
        if (solicitud.getMonto() % multiploGiro != 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser múltiplo de $" + multiploGiro);
        }
        if (solicitud.getMonto() > limiteMaximoGiro) {
            throw new IllegalArgumentException("El monto excede el límite máximo por giro de $" + limiteMaximoGiro);
        }

        Map<String, Object> cuentaActualizada = coreClient.ejecutarRetiro(cuentaId, solicitud.getMonto(), solicitud.getTerminalId());
        long saldoRestante = Long.parseLong(cuentaActualizada.get("saldoContable").toString()) +
                Long.parseLong(cuentaActualizada.get("lineaSobregiro").toString());

        return new RespuestaRetiroDto(
                cuentaId,
                solicitud.getMonto(),
                saldoRestante,
                "APROBADO_DISPENSADO",
                solicitud.getTerminalId(),
                LocalDateTime.now()
        );
    }
}
