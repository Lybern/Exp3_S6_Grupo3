package cl.duoc.bancoxyz.bff.web.client;

import cl.duoc.bancoxyz.bff.web.dto.TransaccionWebDto;
import cl.duoc.bancoxyz.bff.web.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class CoreBancarioClient {

    private static final Logger log = LoggerFactory.getLogger(CoreBancarioClient.class);
    private final RestClient coreRestClient;
    private final JwtTokenUtil jwtTokenUtil;

    public CoreBancarioClient(RestClient coreRestClient, JwtTokenUtil jwtTokenUtil) {
        this.coreRestClient = coreRestClient;
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

    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        String serviceToken = getServiceToken();
        log.info("[BFF-WEB-CLIENT] Consultando cuenta {} en Core con Service Token", cuentaId);

        return coreRestClient.get()
                .uri("/cuentas/{id}", cuentaId)
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public List<Map<String, Object>> obtenerTodasLasCuentas() {
        String serviceToken = getServiceToken();

        return coreRestClient.get()
                .uri("/cuentas/todas")
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public List<TransaccionWebDto> obtenerTransaccionesPorCuenta(Long cuentaId) {
        String serviceToken = getServiceToken();

        return coreRestClient.get()
                .uri("/cuentas/{id}/transacciones", cuentaId)
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionWebDto>>() {});
    }

    public List<Map<String, Object>> obtenerMovimientosAnualesPorCuenta(Long cuentaId) {
        String serviceToken = getServiceToken();

        return coreRestClient.get()
                .uri("/cuentas/{id}/anuales", cuentaId)
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
