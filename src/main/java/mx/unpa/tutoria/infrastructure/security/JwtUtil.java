package mx.unpa.tutoria.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.infrastructure.entity.AdminUsuarioEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Genera y valida tokens JWT para los administradores del CIT.
 */
@Slf4j
@Component
public class JwtUtil {

    private final Key signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:28800000}") long expirationMs) { // 8 h por defecto

        this.signingKey  = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ──────────────────────────────────────────────────────────
    //  Generación
    // ──────────────────────────────────────────────────────────

    public String generarToken(AdminUsuarioEntity usuario) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(usuario.getCorreo())
                .claim("nombre", usuario.getNombre())
                .claim("rol",    usuario.getRol().name())
                .claim("id",     usuario.getId())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    // ──────────────────────────────────────────────────────────
    //  Validación
    // ──────────────────────────────────────────────────────────

    /**
     * Devuelve los claims si el token es válido, null si es inválido o expirado.
     */
    public Claims parsearToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT inválido: {}", e.getMessage());
        }
        return null;
    }

    public String extraerCorreo(String token) {
        Claims claims = parsearToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public boolean esValido(String token) {
        return parsearToken(token) != null;
    }
}
