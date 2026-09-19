package cl.duoc.bancoxyz.bff.cajero;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.bancoxyz.bff.cajero.controller.AuthController;
import cl.duoc.bancoxyz.bff.cajero.dto.LoginRequestDto;
import cl.duoc.bancoxyz.bff.cajero.dto.LoginResponseDto;
import cl.duoc.bancoxyz.bff.cajero.security.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class BffCajeroSecurityTests {

    @Autowired
    private AuthController authController;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Login en BFF Cajero ATM emite JWT con audiencia ATM")
    void loginCajeroExitoso() {
        LoginRequestDto req = new LoginRequestDto("operador_atm", "atm123");
        ResponseEntity<?> resp = authController.login(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponseDto body = (LoginResponseDto) resp.getBody();
        assertThat(body.getToken()).isNotBlank();
        assertThat(body.getCanal()).isEqualTo("ATM");

        UserDetails user = userDetailsService.loadUserByUsername("operador_atm");
        assertThat(jwtTokenUtil.validateToken(body.getToken(), user)).isTrue();
    }
}
