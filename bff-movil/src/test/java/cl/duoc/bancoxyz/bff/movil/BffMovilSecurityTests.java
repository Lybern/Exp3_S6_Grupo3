package cl.duoc.bancoxyz.bff.movil;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.bancoxyz.bff.movil.controller.AuthController;
import cl.duoc.bancoxyz.bff.movil.dto.LoginRequestDto;
import cl.duoc.bancoxyz.bff.movil.dto.LoginResponseDto;
import cl.duoc.bancoxyz.bff.movil.security.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class BffMovilSecurityTests {


    @Autowired
    private AuthController authController;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Login en BFF Móvil emite JWT con audiencia MOVIL")
    void loginMovilExitoso() {
        LoginRequestDto req = new LoginRequestDto("usuario_movil", "movil123");
        ResponseEntity<?> resp = authController.login(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponseDto body = (LoginResponseDto) resp.getBody();
        assertThat(body.getToken()).isNotBlank();
        assertThat(body.getCanal()).isEqualTo("MOVIL");

        UserDetails user = userDetailsService.loadUserByUsername("usuario_movil");
        assertThat(jwtTokenUtil.validateToken(body.getToken(), user)).isTrue();
    }

    @Test
    @DisplayName("Login con contraseña incorrecta devuelve HTTP 401")
    void loginMovilInvalido() {
        LoginRequestDto req = new LoginRequestDto("usuario_movil", "pass_invalida");
        ResponseEntity<?> resp = authController.login(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Validar claims y rol desde token")
    void validarTokenClaims() {
        LoginRequestDto req = new LoginRequestDto("usuario_movil", "movil123");
        ResponseEntity<?> resp = authController.login(req);
        LoginResponseDto body = (LoginResponseDto) resp.getBody();

        String token = body.getToken();
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String rol = jwtTokenUtil.getRoleFromToken(token);
        Claims claims = jwtTokenUtil.getClaimsFromToken(token);

        System.out.println("DEBUG USERNAME: " + username);
        System.out.println("DEBUG ROL: " + rol);
        System.out.println("DEBUG AUD: " + claims.getAudience());
        System.out.println("DEBUG AUD CLASS: " + (claims.getAudience() != null ? claims.getAudience().getClass() : "null"));

        assertThat(username).isEqualTo("usuario_movil");
        assertThat(rol).isEqualTo("ROLE_MOVIL");
        assertThat(claims.getAudience()).contains("MOVIL");
    }
}


