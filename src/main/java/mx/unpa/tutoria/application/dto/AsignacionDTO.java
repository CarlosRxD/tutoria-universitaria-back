package mx.unpa.tutoria.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.unpa.tutoria.domain.model.EstadoAsignacion;
import mx.unpa.tutoria.domain.model.TipoAsignacion;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionDTO {
    private Long id;
    private String periodo;
    private Long alumnoId;
    private String nombreAlumno;
    private String matriculaAlumno;
    private Long docenteId;
    private String nombreDocente;
    private LocalDateTime fechaAsignacion;
    private TipoAsignacion tipo;
    private EstadoAsignacion estado;
}
