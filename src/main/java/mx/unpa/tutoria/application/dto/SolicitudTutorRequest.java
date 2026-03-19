package mx.unpa.tutoria.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudTutorRequest {
    private Long alumnoId;
    private Long docenteId;
    private String periodo;
}
