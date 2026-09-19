package cl.duoc.bancoxyz.bff.movil.controller;

import cl.duoc.bancoxyz.bff.movil.dto.LoginRequestDto;
import cl.duoc.bancoxyz.bff.movil.dto.LoginResponseDto;
import cl.duoc.bancoxyz.bff.movil.security.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación Móvil", description = "Endpoints de login JWT para App Móvil")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Operation(summary = "Login para App Móvil")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);

            LoginResponseDto resp = LoginResponseDto.builder()
                    .token(token)
                    .tipoToken("Bearer")
                    .username(userDetails.getUsername())
                    .canal("MOVIL")
                    .rol("ROLE_MOVIL")
                    .expiracionMilisegundos(3600000)
                    .build();

            return ResponseEntity.ok(resp);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "mensaje", "Credenciales inválidas para App Móvil"));
        }
    }
}
