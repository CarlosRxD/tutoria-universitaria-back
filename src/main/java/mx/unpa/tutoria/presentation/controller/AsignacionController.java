package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.application.dto.AsignacionDTO;
import mx.unpa.tutoria.application.dto.SolicitudTutorRequest;
import mx.unpa.tutoria.application.service.AsignacionTutorService;
import mx.unpa.tutoria.infrastructure.entity.AsignacionEntity;
import mx.unpa.tutoria.infrastructure.repository.AsignacionRepository;
import mx.unpa.tutoria.infrastructure.repository.SolicitudTutorRepository;
import mx.unpa.tutoria.infrastructure.entity.SolicitudTutorEntity;
import mx.unpa.tutoria.infrastructure.repository.AlumnoRepository;
import mx.unpa.tutoria.infrastructure.repository.DocenteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:4200")
public class AsignacionController {

    private final AsignacionTutorService asignacionService;
    private final AsignacionRepository asignacionRepository;
    private final SolicitudTutorRepository solicitudRepository;
    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;

    /**
     * Procesa las asignaciones para un periodo
     */
    @PostMapping("/procesar/{periodo}")
    @Transactional
    public ResponseEntity<String> procesarAsignaciones(@PathVariable String periodo) {
        try {
            asignacionService.procesarAsignacionesPeriodo(periodo);
            return ResponseEntity.ok("Asignaciones procesadas exitosamente para el periodo " + periodo);
        } catch (Exception e) {
            log.error("Error procesando asignaciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error al procesar asignaciones: " + e.getMessage());
        }
    }

    /**
     * Obtiene todas las asignaciones de un periodo
     */
    @GetMapping("/periodo/{periodo}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AsignacionDTO>> obtenerAsignacionesPorPeriodo(
            @PathVariable String periodo) {
        
        List<AsignacionEntity> asignaciones = asignacionRepository.findByPeriodo(periodo);
        List<AsignacionDTO> dtos = asignaciones.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtiene las asignaciones de un docente en un periodo
     */
    @GetMapping("/docente/{docenteId}/periodo/{periodo}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AsignacionDTO>> obtenerAsignacionesDocente(
            @PathVariable Long docenteId,
            @PathVariable String periodo) {
        
        List<AsignacionEntity> asignaciones = 
            asignacionRepository.findByDocenteIdAndPeriodo(docenteId, periodo);
        
        List<AsignacionDTO> dtos = asignaciones.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Registra una solicitud de tutor de un alumno
     */
    @PostMapping("/solicitar")
    public ResponseEntity<String> solicitarTutor(@RequestBody SolicitudTutorRequest request) {
        try {
            var alumno = alumnoRepository.findById(request.getAlumnoId())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            var docente = docenteRepository.findById(request.getDocenteId())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            // Verificar si ya existe una solicitud
            var solicitudExistente = solicitudRepository
                .findByAlumnoIdAndPeriodo(request.getAlumnoId(), request.getPeriodo());
            
            SolicitudTutorEntity solicitud;
            if (solicitudExistente.isPresent()) {
                // Actualizar solicitud existente
                solicitud = solicitudExistente.get();
                solicitud.setDocente(docente);
            } else {
                // Crear nueva solicitud
                solicitud = new SolicitudTutorEntity();
                solicitud.setAlumno(alumno);
                solicitud.setDocente(docente);
                solicitud.setPeriodo(request.getPeriodo());
            }
            
            solicitudRepository.save(solicitud);
            
            return ResponseEntity.ok("Solicitud registrada exitosamente");
            
        } catch (Exception e) {
            log.error("Error registrando solicitud: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error al registrar solicitud: " + e.getMessage());
        }
    }

    /**
     * Obtiene el conteo de tutorados de un docente en un periodo
     */
    @GetMapping("/docente/{docenteId}/periodo/{periodo}/conteo")
    @Transactional(readOnly = true)
    public ResponseEntity<Long> contarTutorados(
            @PathVariable Long docenteId,
            @PathVariable String periodo) {
        
        Long conteo = asignacionService.contarTutorados(docenteId, periodo);
        return ResponseEntity.ok(conteo);
    }

    /**
     * Filtra asignaciones por carrera
     */
    @GetMapping("/periodo/{periodo}/carrera/{carreraId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AsignacionDTO>> filtrarPorCarrera(
            @PathVariable String periodo,
            @PathVariable Integer carreraId) {
        
        List<AsignacionEntity> asignaciones = 
            asignacionRepository.findByPeriodoYCarrera(periodo, carreraId);
        
        List<AsignacionDTO> dtos = asignaciones.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Convierte una entidad a DTO
     */
    private AsignacionDTO convertirADTO(AsignacionEntity entity) {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setId(entity.getId());
        dto.setPeriodo(entity.getPeriodo());
        dto.setAlumnoId(entity.getAlumno().getId());
        dto.setNombreAlumno(entity.getAlumno().getNombreCompleto());
        String matricula = entity.getAlumno().getMatriculas().isEmpty()
                ? "Sin matrícula"
                : entity.getAlumno().getMatriculas().iterator().next().getMatricula();
        dto.setMatriculaAlumno(matricula);
        dto.setDocenteId(entity.getDocente().getId());
        dto.setNombreDocente(entity.getDocente().getNombreCompleto());
        dto.setFechaAsignacion(entity.getFechaAsignacion());
        dto.setTipo(entity.getTipo());
        dto.setEstado(entity.getEstado());
        return dto;
    }
}
