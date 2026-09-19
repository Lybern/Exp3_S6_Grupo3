package cl.duoc.bancoxyz.bff.cajero.client;

import cl.duoc.bancoxyz.bff.cajero.security.JwtTokenUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
                org.springframework.security.core.userdetails.User.withUsername("bff-cajero-client")
                        .password("").roles("ATM").build()
        );
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerCuentaPorIdFallback")
    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        try {
            String serviceToken = getServiceToken();
            log.info("[BFF-CAJERO-CLIENT] Consultando saldo de cuenta {} en Core ({})", cuentaId, coreUrl);

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
        log.warn("[FALLBACK-CAJERO] Circuito activado al consultar cuenta {}. Motivo: {}", cuentaId, t.getMessage());
        return Map.of(
                "id", cuentaId,
                "numeroCuenta", "FALLBACK-" + cuentaId,
                "tipoCuenta", "CUENTA_CORRIENTE",
                "saldoContable", 0L,
                "lineaSobregiro", 0L,
                "nombreTitular", "Usuario ATM (Modo Degradado)",
                "estado", "DEGRADADO_FALLBACK",
                "mensajeFallback", "El cajero automático se encuentra operando en modo contingencia (Core no disponible)."
        );
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "ejecutarRetiroFallback")
    public Map<String, Object> ejecutarRetiro(Long cuentaId, Long monto, String terminalId) {
        try {
            String serviceToken = getServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "cuentaId", cuentaId,
                    "monto", monto,
                    "canal", "ATM-" + terminalId
            );
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    coreUrl + "/operaciones/retiro",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            return ejecutarRetiroFallback(cuentaId, monto, terminalId, e);
        }
    }

    public Map<String, Object> ejecutarRetiroFallback(Long cuentaId, Long monto, String terminalId, Throwable t) {
        log.warn("[FALLBACK-CAJERO] Circuito activado al procesar retiro en terminal {}. Motivo: {}",
                terminalId, t.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El cajero automático no puede dispensar dinero en este momento. Servicio Core no disponible."
        );
    }
}
