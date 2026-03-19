package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "periodosescolares")
@IdClass(PeriodoEscolarEntity.PeriodoId.class)
public class PeriodoEscolarEntity {

    @Id
    @Column(name = "Id_Cic_FK", length = 9)
    private String ciclo;

    @Id
    @Column(name = "Id_Per", length = 1)
    private String periodo;

    @Column(name = "Fechainicio_Per")
    private LocalDate fechaInicio;

    @Column(name = "Fechafin_Per")
    private LocalDate fechaFin;

    // Clase interna obligatoria para llaves primarias compuestas en JPA
    @Data
    public static class PeriodoId implements Serializable {
        private String ciclo;
        private String periodo;
    }
}