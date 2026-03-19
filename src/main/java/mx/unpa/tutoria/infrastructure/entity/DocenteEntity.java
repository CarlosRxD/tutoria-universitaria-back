package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import mx.unpa.tutoria.domain.model.EstadoDocente;

import java.time.LocalDateTime;

/**
 * Entidad local de docente.
 *
 * REGLA DE ORO:
 *  - sice_id  = Id_Tra del SICE → llave de reconciliación, NUNCA cambia.
 *  - correo   = nullable (muchos trabajadores del SICE no tienen email registrado).
 *  - estado   = tiene_override: si es true, SincronizacionEspejoService NO sobreescribe estado.
 *  - correo_override: si es true, SincronizacionEspejoService NO sobreescribe correo.
 */
@Data
@Entity
@Table(
        name = "docentes",
        indexes = {
                @Index(name = "idx_docente_sice_id",   columnList = "sice_id"),
                @Index(name = "idx_docente_correo",    columnList = "correo"),
                @Index(name = "idx_docente_estado",    columnList = "estado")
        }
)
public class DocenteEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del trabajador en el SICE (trabajadores.Id_Tra).
     * Es la llave de reconciliación con el SICE.
     * VARCHAR(5) — nunca cambia entre sincronizaciones.
     */
    @Column(name = "sice_id", length = 5, unique = true)
    private String siceId;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    /**
     * Correo del SICE (Email_Tra). Puede ser null si el SICE no tiene registrado.
     * Unique solo cuando no es null — usamos unique = false y controlamos en el servicio.
     */
    @Column(name = "correo")
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoDocente estado;

    @ManyToOne
    @JoinColumn(name = "carrera_principal_id")
    private CarreraEntity carreraPrincipal;

    @Column(name = "max_tutorados")
    private Integer maxTutorados = 13;

    /**
     * TRUE = la administradora editó este docente manualmente.
     * Cuando es true, SincronizacionEspejoService NO sobreescribe:
     *  - correo
     *  - estado (a menos que el SICE lo marque BAJA definitiva)
     */
    @Column(name = "tiene_override", nullable = false)
    private boolean tieneOverride = false;

    /**
     * Categoría del docente desde el SICE (A, B o C).
     */
    @Column(name = "categoria_sice", length = 1)
    private String categoriaSice;

    /**
     * Nivel del docente desde el SICE (TITULAR / ASOCIADO).
     */
    @Column(name = "nivel_sice", length = 50)
    private String nivelSice;

    /**
     * Última vez que fue sincronizado desde el SICE.
     */
    @Column(name = "ultima_sync")
    private java.time.LocalDateTime ultimaSync;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


}