package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "afinidad_carreras")
public class AfinidadCarrerasEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carrera_origen_id", nullable = false)
    private CarreraEntity carreraOrigen;

    @ManyToOne
    @JoinColumn(name = "carrera_destino_id", nullable = false)
    private CarreraEntity carreraDestino;

    @Column(name = "nivel_prioridad")
    private Integer nivelPrioridad = 1;
}
