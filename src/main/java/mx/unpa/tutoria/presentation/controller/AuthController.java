package mx.unpa.tutoria.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.application.dto.LoginRequest;
import mx.unpa.tutoria.application.dto.LoginResponse;
import mx.unpa.tutoria.infrastructure.entity.AdminUsuarioEntity;
import mx.unpa.tutoria.infrastructure.repository.AdminUsuarioRepository;
import mx.unpa.tutoria.infrastructure.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Endpoint de autenticación para el panel web del CIT.
 *
 * POST /api/auth/login → valida correo + contraseña → devuelve JWT
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminUsuarioRepository adminRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Autentica a un administrador del CIT y devuelve un token JWT.
     *
     * Body esperado: { "correo": "...", "password": "..." }
     * Respuesta 200: LoginResponse con token, correo, nombre, rol, id, expiresIn
     * Respuesta 401: si el correo no existe, el usuario está inactivo o la contraseña es incorrecta.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {

        String correoNorm = req.getCorreo().trim().toLowerCase();
        log.info("Intento de login: {}", correoNorm);

        Optional<AdminUsuarioEntity> usuarioOpt = adminRepo.findByCorreoAndActivoTrue(correoNorm);

        if (usuarioOpt.isEmpty()) {
            log.warn("Login fallido — correo no encontrado o inactivo: {}", correoNorm);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Correo o contraseña incorrectos."));
        }

        AdminUsuarioEntity usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(req.getPassword(), usuario.getPassword())) {
            log.warn("Login fallido — contraseña incorrecta para: {}", correoNorm);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Correo o contraseña incorrectos."));
        }

        String token = jwtUtil.generarToken(usuario);

        LoginResponse resp = LoginResponse.builder()
                .token(token)
                .correo(usuario.getCorreo())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .id(usuario.getId())
                .expiresIn(jwtUtil.getExpirationSeconds())
                .build();

        log.info("Login exitoso: {} ({})", usuario.getNombre(), usuario.getRol());
        return ResponseEntity.ok(resp);
    }

    /**
     * Endpoint de prueba para verificar que un token sigue siendo válido.
     * El cliente puede llamarlo periódicamente para detectar expiraciones.
     */
    @GetMapping("/verificar")
    public ResponseEntity<?> verificar() {
        // Si el JwtAuthFilter dejó pasar la petición con auth context,
        // el token es válido. Si no, Spring Security devolverá 401 antes de llegar aquí.
        return ResponseEntity.ok(Map.of("valido", true));
    }
}
