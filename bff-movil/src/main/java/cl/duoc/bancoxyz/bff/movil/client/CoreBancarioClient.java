package cl.duoc.bancoxyz.bff.movil.client;

import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import cl.duoc.bancoxyz.bff.movil.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
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
        // Token por defecto para llamadas iniciales
        return jwtTokenUtil.generateServiceToken(
                org.springframework.security.core.userdetails.User.withUsername("bff-movil-client")
                        .password("").roles("MOVIL").build()
        );
    }

    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        String serviceToken = getServiceToken();
        log.info("[BFF-MOVIL-CLIENT] Consultando cuenta {} en Core con Service Token", cuentaId);

        return coreRestClient.get()
                .uri("/cuentas/{id}", cuentaId)
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public List<TransaccionMovilDto> obtenerTransaccionesPorCuenta(Long cuentaId) {
        String serviceToken = getServiceToken();

        return coreRestClient.get()
                .uri("/cuentas/{id}/transacciones", cuentaId)
                .header("Authorization", "Bearer " + serviceToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionMovilDto>>() {});
    }

    public void ejecutarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Long monto, String comentario) {
        String serviceToken = getServiceToken();

        Map<String, Object> body = Map.of(
                "cuentaOrigenId", cuentaOrigenId,
                "cuentaDestinoId", cuentaDestinoId,
                "monto", monto,
                "comentario", comentario != null ? comentario : "Transferencia móvil"
        );

        coreRestClient.post()
                .uri("/operaciones/transferencia")
                .header("Authorization", "Bearer " + serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
