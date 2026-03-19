package mx.unpa.tutoria.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlumnoDTO {
    private Long id;
    private String nombreCompleto;
    private String correo;
    private String matriculaActiva;
    private String carreraActual;
    private Integer semestreActual;
}
