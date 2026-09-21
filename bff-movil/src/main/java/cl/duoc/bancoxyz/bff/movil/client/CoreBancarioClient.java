package cl.duoc.bancoxyz.bff.movil.client;

import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import cl.duoc.bancoxyz.bff.movil.security.JwtTokenUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class CoreBancarioClient {

    private static final Logger log = LoggerFactory.getLogger(CoreBancarioClient.class);
    private final RestTemplate restTemplate;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${bank.core.url:http://core-service/api/core}")
    private String coreUrl;

    public CoreBancarioClient(RestTemplate restTemplate, JwtTokenUtil jwtTokenUtil) {
        this.restTemplate = restTemplate;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    private String getServiceToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            return jwtTokenUtil.generateServiceToken(userDetails);
        }
        return jwtTokenUtil.generateServiceToken(
                org.springframework.security.core.userdetails.User.withUsername("bff-movil-client")
                        .password("").roles("MOVIL").build()
        );
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerCuentaPorIdFallback")
    @Retry(name = "coreServiceCB")
    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        try {
            String serviceToken = getServiceToken();
            log.info("[BFF-MOVIL-CLIENT] Consultando cuenta {} en Core ({}) con Service Token", cuentaId, coreUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    coreUrl + "/cuentas/" + cuentaId,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            return obtenerCuentaPorIdFallback(cuentaId, e);
        }
    }

    public Map<String, Object> obtenerCuentaPorIdFallback(Long cuentaId, Throwable t) {
        log.warn("[FALLBACK-MOVIL] Circuito activado al consultar cuenta {}. Motivo: {}", cuentaId, t.getMessage());
        return Map.of(
                "id", cuentaId,
                "numeroCuenta", "FALLBACK-" + cuentaId,
                "tipoCuenta", "ahorro",
                "saldoContable", 0L,
                "lineaSobregiro", 0L,
                "saldoDisponible", 0L,
                "nombreTitular", "Usuario Móvil (Modo Degradado)",
                "estado", "DEGRADADO_FALLBACK",
                "mensajeFallback", "El servicio central Core no responde. Circuito abierto / Fallback activo."
        );
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerTransaccionesPorCuentaFallback")
    @Retry(name = "coreServiceCB")
    public List<TransaccionMovilDto> obtenerTransaccionesPorCuenta(Long cuentaId) {
        try {
            String serviceToken = getServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<TransaccionMovilDto>> response = restTemplate.exchange(
                    coreUrl + "/cuentas/" + cuentaId + "/transacciones",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<TransaccionMovilDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            return obtenerTransaccionesPorCuentaFallback(cuentaId, e);
        }
    }

    public List<TransaccionMovilDto> obtenerTransaccionesPorCuentaFallback(Long cuentaId, Throwable t) {
        log.warn("[FALLBACK-MOVIL] Circuito activado al obtener transacciones de cuenta {}. Motivo: {}", cuentaId, t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "ejecutarTransferenciaFallback")
    @Retry(name = "coreServiceCB")
    public void ejecutarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Long monto, String comentario) {
        try {
            String serviceToken = getServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "cuentaOrigenId", cuentaOrigenId,
                    "cuentaDestinoId", cuentaDestinoId,
                    "monto", monto,
                    "comentario", comentario != null ? comentario : "Transferencia móvil"
            );
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            restTemplate.exchange(
                    coreUrl + "/operaciones/transferencia",
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            ejecutarTransferenciaFallback(cuentaOrigenId, cuentaDestinoId, monto, comentario, e);
        }
    }

    public void ejecutarTransferenciaFallback(Long cuentaOrigenId, Long cuentaDestinoId, Long monto, String comentario, Throwable t) {
        log.warn("[FALLBACK-MOVIL] Circuito activado al transferir fondos de {} a {}. Motivo: {}",
                cuentaOrigenId, cuentaDestinoId, t.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio de transferencias temporalmente no disponible (Circuit Breaker activado). Intente más tarde."
        );
    }
}
