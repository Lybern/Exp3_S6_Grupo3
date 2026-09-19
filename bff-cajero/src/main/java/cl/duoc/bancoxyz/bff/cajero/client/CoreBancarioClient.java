package cl.duoc.bancoxyz.bff.cajero.client;

import cl.duoc.bancoxyz.bff.cajero.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
                org.springframework.security.core.userdetails.User.withUsername("bff-cajero-client")
                        .password("").roles("ATM").build()
        );
    }

    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        String serviceToken = getServiceToken();
        log.info("[BFF-CAJERO-CLIENT] Consultando saldo de cuenta {} en Core", cuentaId);

        return coreRestClient.get()
                .uri("/cuentas/{id}", cuentaId)
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> ejecutarRetiro(Long cuentaId, Long monto, String terminalId) {
        String serviceToken = getServiceToken();

        Map<String, Object> body = Map.of(
                "cuentaId", cuentaId,
                "monto", monto,
                "canal", "ATM-" + terminalId
        );

        return coreRestClient.post()
                .uri("/operaciones/retiro")
                .header("Authorization", "Bearer " + serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
