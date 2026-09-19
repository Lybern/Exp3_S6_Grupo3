package cl.duoc.bancoxyz.core.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenFilter.class);
    private final ServiceTokenUtil serviceTokenUtil;

    public ServiceTokenFilter(ServiceTokenUtil serviceTokenUtil) {
        this.serviceTokenUtil = serviceTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Endpoints publicos Swagger
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = serviceTokenUtil.parseToken(token);
                request.setAttribute("claims", claims);
                log.info("[CORE-AUTH] Service Token validado exitosamente. Emisor: '{}', Sujeto: '{}', Aud: '{}'",
                        claims.getIssuer(), claims.getSubject(), claims.getAudience());
                filterChain.doFilter(request, response);
                return;
            } catch (Exception e) {
                log.warn("[CORE-AUTH] Service Token rechazado o invalido: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Service Token invalido o corrupto");
                return;
            }
        }

        // Si no trae header de autorizacion
        log.warn("[CORE-AUTH] Acceso denegado a {}: Cabecera Authorization ausente", path);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Se requiere Service Token en cabecera Authorization: Bearer <token>");
    }
}
