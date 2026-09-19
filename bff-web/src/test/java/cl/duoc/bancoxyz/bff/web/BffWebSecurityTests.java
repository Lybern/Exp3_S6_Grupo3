package cl.duoc.bancoxyz.bff.web;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.bancoxyz.bff.web.controller.AuthController;
import cl.duoc.bancoxyz.bff.web.dto.LoginRequestDto;
import cl.duoc.bancoxyz.bff.web.dto.LoginResponseDto;
import cl.duoc.bancoxyz.bff.web.security.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class BffWebSecurityTests {

    @Autowired
    private AuthController authController;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Login en BFF Web emite JWT con audiencia WEB")
    void loginWebExitoso() {
        LoginRequestDto req = new LoginRequestDto("usuario_web", "web123");
        ResponseEntity<?> resp = authController.login(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponseDto body = (LoginResponseDto) resp.getBody();
        assertThat(body.getToken()).isNotBlank();
        assertThat(body.getCanal()).isEqualTo("WEB");

        UserDetails user = userDetailsService.loadUserByUsername("usuario_web");
        assertThat(jwtTokenUtil.validateToken(body.getToken(), user)).isTrue();
    }
}
