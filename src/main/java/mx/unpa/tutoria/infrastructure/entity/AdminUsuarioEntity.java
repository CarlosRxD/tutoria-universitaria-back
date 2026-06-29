package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Usuarios que pueden acceder al panel web de administración del CIT.
 * No confundir con la tabla 'docentes' (datos del SICE).
 */
@Data
@Entity
@Table(name = "admin_usuarios")
public class AdminUsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    /** BCrypt hash de la contraseña. Nunca se devuelve al cliente. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolAdmin rol;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum RolAdmin {
        ADMIN, COORDINADOR
    }
}
