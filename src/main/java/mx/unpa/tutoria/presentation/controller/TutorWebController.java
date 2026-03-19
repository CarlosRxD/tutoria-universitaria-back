package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.domain.model.TipoAsignacion;
import mx.unpa.tutoria.infrastructure.entity.*;
import mx.unpa.tutoria.infrastructure.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para el Frontend Web - Gestión de Tutorías
 */
@Slf4j
@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class TutorWebController {

    private final SolicitudTutorRepository solicitudRepository;
    private final AsignacionRepository asignacionRepository;
    private final AlumnoMatriculaRepository matriculaRepository;
    private final ConfiguracionRepository configuracionRepository;
    /**
     * Obtiene todas las solicitudes de cambio de tutor para un periodo
     */
    @GetMapping("/solicitudes-tutor")
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerSolicitudes(@RequestParam String periodo) {
        try {
            List<SolicitudTutorEntity> solicitudes = solicitudRepository
                    .findByPeriodo(periodo);

            var solicitudesDTO = solicitudes.stream()
                    .map(s -> {
                        // Obtener la primera matrícula del alumno
                        String matricula = s.getAlumno().getMatriculas().isEmpty()
                                ? "Sin matrícula"
                                : s.getAlumno().getMatriculas().iterator().next().getMatricula();

                        return Map.of(
                                "id", s.getId(),
                                "alumnoNombre", s.getAlumno().getNombreCompleto(),
                                "alumnoMatricula", matricula,
                                "docenteNombre", s.getDocente().getNombreCompleto(),
                                "periodo", s.getPeriodo(),
                                "fechaSolicitud", s.getFechaSolicitud().toString()
                        );
                    })
                    .collect(Collectors.toList());

            log.info("Solicitudes encontradas para periodo {}: {}", periodo, solicitudesDTO.size());

            return ResponseEntity.ok(solicitudesDTO);

        } catch (Exception e) {
            log.error("Error obteniendo solicitudes: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al obtener solicitudes",
                    "detalle", e.getMessage()
            ));
        }
    }

    /**
     * Procesa todas las solicitudes y genera asignaciones
     */
    @PostMapping("/procesar-asignaciones")
    public ResponseEntity<?> procesarAsignaciones(@RequestParam String periodo) {
        try {
            List<SolicitudTutorEntity> solicitudes = solicitudRepository
                    .findByPeriodo(periodo);

            if (solicitudes.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "exito", true,
                        "mensaje", "No hay solicitudes para procesar",
                        "totalProcesadas", 0
                ));
            }

            int procesadas = 0;
            int actualizadas = 0;
            int nuevas = 0;

            for (SolicitudTutorEntity solicitud : solicitudes) {
                try {
                    // Buscar asignación existente
                    var asignacionOpt = asignacionRepository
                            .findByAlumnoIdAndPeriodo(solicitud.getAlumno().getId(), periodo);

                    AsignacionEntity asignacion;
                    if (asignacionOpt.isPresent()) {
                        // Actualizar asignación existente
                        asignacion = asignacionOpt.get();
                        asignacion.setDocente(solicitud.getDocente());
                        asignacion.setTipo(TipoAsignacion.ELECCION_ALUMNO);
                        actualizadas++;
                        log.info("Asignación actualizada: Alumno {} -> Tutor {}",
                                solicitud.getAlumno().getNombreCompleto(),
                                solicitud.getDocente().getNombreCompleto());
                    } else {
                        // Crear nueva asignación
                        asignacion = new AsignacionEntity();
                        asignacion.setAlumno(solicitud.getAlumno());
                        asignacion.setDocente(solicitud.getDocente());
                        asignacion.setPeriodo(periodo);
                        asignacion.setTipo(TipoAsignacion.ELECCION_ALUMNO);
                        asignacion.setFechaAsignacion(LocalDateTime.now());
                        nuevas++;
                        log.info("Asignación creada: Alumno {} -> Tutor {}",
                                solicitud.getAlumno().getNombreCompleto(),
                                solicitud.getDocente().getNombreCompleto());
                    }

                    asignacionRepository.save(asignacion);
                    procesadas++;

                } catch (Exception e) {
                    log.error("Error procesando solicitud {}: {}", solicitud.getId(), e.getMessage());
                }
            }

            log.info("Procesamiento completado: {} procesadas ({} nuevas, {} actualizadas) de {} solicitudes",
                    procesadas, nuevas, actualizadas, solicitudes.size());

            return ResponseEntity.ok(Map.of(
                    "exito", true,
                    "mensaje", String.format("Procesadas %d asignaciones (%d nuevas, %d actualizadas)",
                            procesadas, nuevas, actualizadas),
                    "totalProcesadas", procesadas,
                    "nuevas", nuevas,
                    "actualizadas", actualizadas,
                    "totalSolicitudes", solicitudes.size()
            ));

        } catch (Exception e) {
            log.error("Error procesando asignaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al procesar asignaciones",
                    "detalle", e.getMessage()
            ));
        }
    }

    /**
     * Obtiene estadísticas de las solicitudes por periodo
     */
    @GetMapping("/estadisticas-solicitudes")
    public ResponseEntity<?> obtenerEstadisticas(@RequestParam String periodo) {
        try {
            List<SolicitudTutorEntity> solicitudes = solicitudRepository.findByPeriodo(periodo);

            long totalSolicitudes = solicitudes.size();
            long procesadas = solicitudes.stream()
                    .filter(s -> asignacionRepository
                            .findByAlumnoIdAndPeriodo(s.getAlumno().getId(), periodo)
                            .isPresent())
                    .count();
            long pendientes = totalSolicitudes - procesadas;

            return ResponseEntity.ok(Map.of(
                    "periodo", periodo,
                    "totalSolicitudes", totalSolicitudes,
                    "procesadas", procesadas,
                    "pendientes", pendientes
            ));

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al obtener estadísticas"
            ));
        }
    }
    /**
     * Obtiene el estado de la ventana de cambio de tutor
     */
    @GetMapping("/estado-ventana")
    public ResponseEntity<Map<String, Boolean>> obtenerEstadoVentana() {
        boolean abierta = configuracionRepository.findById("VENTANA_ASIGNACION")
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(false);
        return ResponseEntity.ok(Map.of("abierta", abierta));
    }

    /**
     * Cambia el estado de la ventana de cambio de tutor (Abrir/Cerrar)
     */
    @PostMapping("/estado-ventana")
    public ResponseEntity<Map<String, Object>> cambiarEstadoVentana(@RequestBody Map<String, Boolean> body) {
        boolean abrir = body.getOrDefault("abierta", false);

        ConfiguracionEntity config = configuracionRepository.findById("VENTANA_ASIGNACION")
                .orElse(new ConfiguracionEntity());
        config.setClave("VENTANA_ASIGNACION");
        config.setValor(String.valueOf(abrir));
        configuracionRepository.save(config);

        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", abrir ? "El periodo de cambio ha INICIADO." : "El periodo de cambio ha sido CERRADO.",
                "abierta", abrir
        ));
    }
}