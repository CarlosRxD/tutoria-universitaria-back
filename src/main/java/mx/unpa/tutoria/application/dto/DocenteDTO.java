package mx.unpa.tutoria.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.unpa.tutoria.domain.model.EstadoDocente;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocenteDTO {
    private Long id;
    private String nombreCompleto;
    private String correo;
    private EstadoDocente estado;
    private String carreraPrincipal;
    private Integer maxTutorados;
    private Integer tutoradosActuales;
}
