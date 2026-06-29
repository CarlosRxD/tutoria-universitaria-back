package mx.unpa.tutoria.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que extrae el JWT del header Authorization y establece
 * el contexto de seguridad de Spring para la petición actual.
 *
 * Solo actúa cuando hay un header "Authorization: Bearer <token>".
 * Si no hay token, deja pasar la petición (la cadena de seguridad
 * decidirá si el endpoint requiere autenticación o no).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = header.substring(7).trim();
        Claims claims = jwtUtil.parsearToken(token);

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            String correo = claims.getSubject();
            String rol    = claims.get("rol", String.class);

            var auth = new UsernamePasswordAuthenticationToken(
                    correo,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol))
            );
            auth.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("JWT válido — usuario: {}, rol: {}", correo, rol);
        }

        chain.doFilter(request, response);
    }
}
