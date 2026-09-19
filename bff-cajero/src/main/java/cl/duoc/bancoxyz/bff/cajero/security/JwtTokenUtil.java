package cl.duoc.bancoxyz.bff.cajero.security;

import cl.duoc.bancoxyz.bff.cajero.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;
    private SecretKey serviceSecretKey;

    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.serviceSecretKey = Keys.hmacShaKeyFor(jwtProperties.getServiceSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        String rol = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_ATM");

        return Jwts.builder()
                .issuer("bff-cajero")
                .subject(userDetails.getUsername())
                .audience().add("ATM").and()
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateServiceToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getServiceExpiration());

        return Jwts.builder()
                .issuer("bff-cajero")
                .subject(userDetails.getUsername())
                .audience().add("core-bancario").and()
                .claim("rol", "ROLE_ATM")
                .claim("tipo", "SERVICE_TOKEN")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(serviceSecretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("rol", String.class);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        Claims claims = getClaimsFromToken(token);
        boolean audOk = claims.getAudience() != null && claims.getAudience().contains("ATM");
        return username.equals(userDetails.getUsername()) && audOk && !claims.getExpiration().before(new Date());
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
