package mx.unpa.tutoria.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta del endpoint POST /api/auth/login.
 * Debe coincidir exactamente con la interfaz LoginResponse del frontend Angular.
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private String correo;
    private String nombre;
    private String rol;       // "ADMIN" | "COORDINADOR"
    private Long   id;
    private long   expiresIn; // segundos hasta expiración
}
