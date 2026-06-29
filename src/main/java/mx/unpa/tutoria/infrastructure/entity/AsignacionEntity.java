package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import mx.unpa.tutoria.domain.model.EstadoAsignacion;
import mx.unpa.tutoria.domain.model.TipoAsignacion;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "asignaciones")
public class AsignacionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String periodo;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private AlumnoEntity alumno;

    @ManyToOne
    @JoinColumn(name = "docente_id", nullable = false)
    private DocenteEntity docente;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoAsignacion tipo;

    // ─────────────────────────────────────────────────────────────
    //  🔒 FIX DE RAÍZ: estado ya nunca podrá entrar como NULL.
    //  - nullable=false  → DDL fuerza NOT NULL
    //  - valor por default en el campo → aunque alguien olvide setEstado()
    //  - @PrePersist     → cinturón + tirantes (ver abajo)
    //  - columnDefinition → coincide con el ALTER de fix-null-estado.sql
    // ─────────────────────────────────────────────────────────────
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'SOLICITADO'")
    private EstadoAsignacion estado = EstadoAsignacion.SOLICITADO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}