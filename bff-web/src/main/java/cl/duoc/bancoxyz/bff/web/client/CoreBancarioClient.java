package cl.duoc.bancoxyz.bff.web.client;

import cl.duoc.bancoxyz.bff.web.dto.TransaccionWebDto;
import cl.duoc.bancoxyz.bff.web.security.JwtTokenUtil;
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
                org.springframework.security.core.userdetails.User.withUsername("bff-web-client")
                        .password("").roles("WEB").build()
        );
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerCuentaPorIdFallback")
    @Retry(name = "coreServiceCB")
    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        try {
            String serviceToken = getServiceToken();
            log.info("[BFF-WEB-CLIENT] Consultando cuenta {} en Core ({}) con Service Token", cuentaId, coreUrl);

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
        log.warn("[FALLBACK-WEB] Circuito activado al consultar cuenta {}. Motivo: {}", cuentaId, t.getMessage());
        return Map.of(
                "id", cuentaId,
                "numeroCuenta", "FALLBACK-" + cuentaId,
                "tipoCuenta", "ahorro",
                "saldoContable", 0L,
                "lineaSobregiro", 0L,
                "tasaInteresAnual", 0.0,
                "nombreTitular", "Usuario Web (Modo Degradado)",
                "edadTitular", 0,
                "estado", "DEGRADADO_FALLBACK",
                "mensajeFallback", "El servicio central Core no responde. Circuito abierto / Fallback activo."
        );
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerTodasLasCuentasFallback")
    @Retry(name = "coreServiceCB")
    public List<Map<String, Object>> obtenerTodasLasCuentas() {
        try {
            String serviceToken = getServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    coreUrl + "/cuentas/todas",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            return obtenerTodasLasCuentasFallback(e);
        }
    }

    public List<Map<String, Object>> obtenerTodasLasCuentasFallback(Throwable t) {
        log.warn("[FALLBACK-WEB] Circuito activado al obtener todas las cuentas. Motivo: {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerTransaccionesPorCuentaFallback")
    @Retry(name = "coreServiceCB")
    public List<TransaccionWebDto> obtenerTransaccionesPorCuenta(Long cuentaId) {
        try {
            String serviceToken = getServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<TransaccionWebDto>> response = restTemplate.exchange(
                    coreUrl + "/cuentas/" + cuentaId + "/transacciones",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<TransaccionWebDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            return obtenerTransaccionesPorCuentaFallback(cuentaId, e);
        }
    }

    public List<TransaccionWebDto> obtenerTransaccionesPorCuentaFallback(Long cuentaId, Throwable t) {
        log.warn("[FALLBACK-WEB] Circuito activado al obtener transacciones de cuenta {}. Motivo: {}", cuentaId, t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "coreServiceCB", fallbackMethod = "obtenerMovimientosAnualesPorCuentaFallback")
    @Retry(name = "coreServiceCB")
    public List<Map<String, Object>> obtenerMovimientosAnualesPorCuenta(Long cuentaId) {
        try {
            String serviceToken = getServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    coreUrl + "/cuentas/" + cuentaId + "/anuales",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            return obtenerMovimientosAnualesPorCuentaFallback(cuentaId, e);
        }
    }

    public List<Map<String, Object>> obtenerMovimientosAnualesPorCuentaFallback(Long cuentaId, Throwable t) {
        log.warn("[FALLBACK-WEB] Circuito activado al obtener movimientos anuales de cuenta {}. Motivo: {}", cuentaId, t.getMessage());
        return Collections.emptyList();
    }
}
