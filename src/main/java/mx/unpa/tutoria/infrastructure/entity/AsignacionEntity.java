package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import mx.unpa.tutoria.domain.model.EstadoAsignacion;
import mx.unpa.tutoria.domain.model.TipoAsignacion;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "asignaciones")
public class AsignacionEntity  extends BaseEntity {

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

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoAsignacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoAsignacion estado;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}