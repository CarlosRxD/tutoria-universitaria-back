package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.application.dto.SolicitudTutorRequest;
import mx.unpa.tutoria.domain.model.TipoAsignacion;
import mx.unpa.tutoria.infrastructure.entity.AsignacionEntity;
import mx.unpa.tutoria.infrastructure.entity.DocenteEntity;
import mx.unpa.tutoria.infrastructure.entity.SolicitudTutorEntity;
import mx.unpa.tutoria.infrastructure.entity.AlumnoEntity;
import mx.unpa.tutoria.infrastructure.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador para la App Móvil - Gestión de Tutores
 */
@Slf4j
@RestController
@RequestMapping("/api/movil/tutor")
@RequiredArgsConstructor
public class TutorMovilController {

    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final SolicitudTutorRepository solicitudRepository;
    private final AsignacionRepository asignacionRepository;
    private final AlumnoMatriculaRepository matriculaRepository;
    private final ConfiguracionRepository configuracionRepository;
    /**
     * Verifica si la ventana de solicitud está abierta
     * Ventana: 1 semana desde el 2 de marzo + cada inicio de semestre
     */
    @GetMapping("/ventana-abierta")
    public ResponseEntity<Map<String, Object>> verificarVentana(@RequestParam String periodo) {

        // Leemos directamente de la base de datos lo que la maestra haya decidido
        boolean abierta = configuracionRepository.findById("VENTANA_ASIGNACION")
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(false);

        String mensaje = abierta
                ? "Ventana de solicitud abierta. ¡Elige a tu nuevo tutor!"
                : "La ventana de solicitud de tutor se encuentra cerrada en este momento. Espera el aviso de coordinación.";

        return ResponseEntity.ok(Map.of(
                "abierta", abierta,
                "mensaje", mensaje,
                "proximaApertura", "Aviso oficial"
        ));
    }
    @GetMapping("/ventana-abierta-test")
    public ResponseEntity<Map<String, Object>> verificarVentanaTest(@RequestParam String periodo) {
        // Siempre retorna ventana abierta para testing
        return ResponseEntity.ok(Map.of(
                "abierta", true,
                "mensaje", "Ventana de solicitud abierta (modo test)",
                "proximaApertura", LocalDate.now().toString()
        ));
    }

    /**
     * Obtiene el tutor actual del alumno
     */
    @GetMapping("/mi-tutor")
    public ResponseEntity<?> obtenerMiTutor(
            @RequestParam String matricula,
            @RequestParam String periodo) {

        // 1. Buscar alumno por matrícula
        var matriculaOpt = matriculaRepository.findByMatricula(matricula);
        if (matriculaOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Alumno no encontrado"));
        }

        Long alumnoId = matriculaOpt.get().getAlumno().getId();

        // 2. Buscar asignación del periodo
        Optional<AsignacionEntity> asignacionOpt =
                asignacionRepository.findByAlumnoIdAndPeriodo(alumnoId, periodo);

        if (asignacionOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "tieneTutor", false,
                    "mensaje", "Aún no tienes tutor asignado para este periodo"
            ));
        }

        AsignacionEntity asignacion = asignacionOpt.get();
        DocenteEntity tutor = asignacion.getDocente();

        return ResponseEntity.ok(Map.of(
                "tieneTutor", true,
                "nombreTutor", tutor.getNombreCompleto(),
                "correoTutor", tutor.getCorreo(),
                "carreraTutor", tutor.getCarreraPrincipal() != null ? tutor.getCarreraPrincipal().getNombre() : "",
                "tipoAsignacion", asignacion.getTipo().toString(),
                "fechaAsignacion", asignacion.getFechaAsignacion().toString()
        ));
    }

    /**
     * Obtiene la lista de docentes disponibles
     * (Filtra por carrera del alumno si es posible)
     */
    @GetMapping("/docentes-disponibles")
    public ResponseEntity<?> obtenerDocentesDisponibles(
            @RequestParam String matricula,
            @RequestParam String periodo) {

        // 1. Obtener carrera del alumno
        var matriculaOpt = matriculaRepository.findByMatricula(matricula);
        if (matriculaOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Alumno no encontrado"));
        }

        Integer carreraId = matriculaOpt.get().getCarrera() != null
                ? matriculaOpt.get().getCarrera().getId()
                : null;

        // 2. Obtener docentes activos (priorizar de su carrera)
        List<DocenteEntity> docentes;
        if (carreraId != null) {
            docentes = docenteRepository.findActivosByCarrera(carreraId);
            // Si no hay suficientes, agregar de otras carreras
            if (docentes.size() < 5) {
                docentes.addAll(docenteRepository.findAllActivos());
            }
        } else {
            docentes = docenteRepository.findAllActivos();
        }

        // 3. Formatear respuesta
        var docentesDTO = docentes.stream()
                .distinct()
                .map(d -> Map.of(
                        "id", d.getId(),
                        "nombre", d.getNombreCompleto(),
                        "correo", d.getCorreo(),
                        "carrera", d.getCarreraPrincipal() != null ? d.getCarreraPrincipal().getNombre() : "Sin carrera",
                        "tutoradosActuales", asignacionRepository.contarTutoradosPorDocenteYPeriodo(d.getId(), periodo),
                        "maxTutorados", d.getMaxTutorados()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(docentesDTO);
    }

    /**
     * Registra solicitud de tutor desde la app móvil
     */
    @PostMapping("/solicitar")
    @Transactional
    public ResponseEntity<?> solicitarTutor(@RequestBody SolicitudMovilRequest request) {
        try {
            // 1. Validar ventana (puedes descomentar esto cuando vayas a producción)
            /*
            var ventana = verificarVentana(request.getPeriodo());
            Map<String, Object> ventanaData = (Map<String, Object>) ventana.getBody();
            if (ventanaData != null && !(Boolean) ventanaData.get("abierta")) {
                return ResponseEntity.ok(Map.of("exito", false, "error", "La ventana de solicitudes está cerrada."));
            }
            */

            // 2. Buscar alumno y docente
            var matriculaOpt = matriculaRepository.findByMatricula(request.getMatricula());
            if (matriculaOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("exito", false, "error", "Alumno no encontrado"));
            AlumnoEntity alumno = matriculaOpt.get().getAlumno();

            var docenteOpt = docenteRepository.findById(request.getDocenteId());
            if (docenteOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("exito", false, "error", "Docente no encontrado"));
            DocenteEntity docente = docenteOpt.get();

            // 🚀 3. NUEVA LÓGICA: Verificar si el maestro ya llegó a su límite (ej. 13)
            long tutoradosActuales = asignacionRepository.contarTutoradosPorDocenteYPeriodo(docente.getId(), request.getPeriodo());
            if (tutoradosActuales >= docente.getMaxTutorados()) {
                // Respondemos con "200 OK" para que Android no crashee, pero le decimos que falló por cupo lleno.
                return ResponseEntity.ok(Map.of(
                        "exito", false,
                        "error", "Este maestro ya alcanzó su límite de " + docente.getMaxTutorados() + " alumnos. Por favor, selecciona a otro tutor."
                ));
            }

            // ✨ 4. MAGIA: Si hay espacio, creamos la ASIGNACIÓN OFICIAL inmediatamente
            var asignacionOpt = asignacionRepository.findByAlumnoIdAndPeriodo(alumno.getId(), request.getPeriodo());
            AsignacionEntity asignacion;
            if (asignacionOpt.isPresent()) {
                asignacion = asignacionOpt.get();
                asignacion.setDocente(docente); // Actualiza si el alumno cambió de opinión
                asignacion.setTipo(TipoAsignacion.ELECCION_ALUMNO);
                asignacion.setFechaAsignacion(LocalDateTime.now());
            } else {
                asignacion = new AsignacionEntity();
                asignacion.setAlumno(alumno);
                asignacion.setDocente(docente);
                asignacion.setPeriodo(request.getPeriodo());
                asignacion.setTipo(TipoAsignacion.ELECCION_ALUMNO);
                asignacion.setFechaAsignacion(LocalDateTime.now());
            }
            asignacionRepository.save(asignacion);

            // 5. Guardamos también la "Solicitud" solo para que aparezca en las estadísticas de tu panel web
            var solicitudExistente = solicitudRepository.findByAlumnoIdAndPeriodo(alumno.getId(), request.getPeriodo());
            SolicitudTutorEntity solicitud = solicitudExistente.orElse(new SolicitudTutorEntity());
            solicitud.setAlumno(alumno);
            solicitud.setDocente(docente);
            solicitud.setPeriodo(request.getPeriodo());
            solicitud.setFechaSolicitud(LocalDateTime.now());
            solicitudRepository.save(solicitud);

            return ResponseEntity.ok(Map.of(
                    "exito", true,
                    "mensaje", "¡Excelente! Has sido asignado exitosamente al tutor " + docente.getNombreCompleto(),
                    "tutorSolicitado", docente.getNombreCompleto(),
                    "periodo", request.getPeriodo()
            ));

        } catch (Exception e) {
            log.error("Error al procesar solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("exito", false, "error", "Error interno del servidor."));
        }
    }
    /**
     * Obtiene el historial de tutores del alumno
     */
    @GetMapping("/historial")
    public ResponseEntity<?> obtenerHistorialTutores(
            @RequestParam String matricula) {

        try {
            // 1. Buscar alumno
            var matriculaOpt = matriculaRepository.findByMatricula(matricula);
            if (matriculaOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Alumno no encontrado"));
            }

            Long alumnoId = matriculaOpt.get().getAlumno().getId();

            // 2. Obtener todas las asignaciones históricas
            List<AsignacionEntity> historial = asignacionRepository
                    .findByAlumnoIdOrderByFechaAsignacionDesc(alumnoId);

            // 3. Formatear respuesta
            var historialDTO = historial.stream()
                    .map(asignacion -> {
                        DocenteEntity tutor = asignacion.getDocente();
                        return Map.of(
                                "periodo", asignacion.getPeriodo(),
                                "nombreTutor", tutor.getNombreCompleto(),
                                "correoTutor", tutor.getCorreo(),
                                "carreraTutor", tutor.getCarreraPrincipal() != null
                                        ? tutor.getCarreraPrincipal().getNombre()
                                        : "Sin carrera",
                                "tipoAsignacion", formatearTipoAsignacion(asignacion.getTipo().toString()),
                                "fechaAsignacion", asignacion.getFechaAsignacion().toString()
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(historialDTO);

        } catch (Exception e) {
            log.error("Error obteniendo historial: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al obtener historial",
                    "detalle", e.getMessage()
            ));
        }
    }
    @PutMapping("/actualizar-correo")
    public ResponseEntity<?> actualizarCorreo(@RequestBody ActualizarCorreoRequest request) {
        try {
            var matriculaOpt = matriculaRepository.findByMatricula(request.getMatricula());
            if (matriculaOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Alumno no encontrado"));
            }

            AlumnoEntity alumno = matriculaOpt.get().getAlumno();

            // Validar formato de correo
            if (!request.getCorreo().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.status(400).body(Map.of("error", "Formato de correo inválido"));
            }

            // Actualizar correo
            alumno.setCorreo(request.getCorreo());
            alumnoRepository.save(alumno);

            log.info("Correo actualizado para alumno {}: {}", request.getMatricula(), request.getCorreo());

            return ResponseEntity.ok(Map.of(
                    "exito", true,
                    "mensaje", "Correo actualizado correctamente",
                    "correo", request.getCorreo()
            ));

        } catch (Exception e) {
            log.error("Error al actualizar correo: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al actualizar correo",
                    "detalle", e.getMessage()
            ));
        }
    }

    /**
     * Formatea el tipo de asignación para mostrar en móvil
     */
    private String formatearTipoAsignacion(String tipo) {
        switch (tipo) {
            case "ELECCION_ALUMNO":   return "Elegido por ti";
            case "CONTINUIDAD":       return "Continuidad";
            case "ALEATORIO_CARRERA": return "Asignación automática";
            case "ALEATORIO_AFIN":    return "Carrera afín";
            case "ALEATORIO_GLOBAL":  return "Asignación general";
            default: return tipo;
        }
    }
    // DTO interno para la solicitud desde móvil
    @lombok.Data
    public static class SolicitudMovilRequest {
        private String matricula;
        private Long docenteId;
        private String periodo;
        private String motivo;  // Opcional
    }
    @lombok.Data
    public static class ActualizarCorreoRequest {
        private String matricula;
        private String correo;
    }
}