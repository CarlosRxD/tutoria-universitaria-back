package mx.unpa.tutoria.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.domain.model.EstadoDocente;
import mx.unpa.tutoria.domain.model.TipoAsignacion;
import mx.unpa.tutoria.infrastructure.entity.*;
import mx.unpa.tutoria.infrastructure.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionTutorService {

    private final AsignacionRepository asignacionRepository;
    private final SolicitudTutorRepository solicitudRepository;
    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoMatriculaRepository matriculaRepository;
    private final AfinidadCarrerasRepository afinidadRepository;

    /**
     * Proceso principal de asignación de tutores para un periodo
     */
    @Transactional
    public void procesarAsignacionesPeriodo(String periodo) {
        log.info("Iniciando proceso de asignación para periodo: {}", periodo);

        // 1. Obtener todos los alumnos activos
        List<AlumnoEntity> todosLosAlumnos = alumnoRepository.findAll();

        // 2. Obtener solicitudes del periodo ordenadas por fecha
        List<SolicitudTutorEntity> solicitudes = solicitudRepository
                .findByPeriodoOrdenadoPorFecha(periodo);

        // 3. Obtener asignaciones del periodo anterior (Para la continuidad)
        String periodoAnterior = calcularPeriodoAnterior(periodo);
        Map<Long, AsignacionEntity> asignacionesAnteriores =
                asignacionRepository.findByPeriodo(periodoAnterior)
                        .stream()
                        .collect(Collectors.toMap(a -> a.getAlumno().getId(), a -> a, (a1, a2) -> a1));

        // 🚨 4. EL ARREGLO: Cargar las asignaciones que YA EXISTEN en este periodo (hechas por la app)
        Map<Long, AsignacionEntity> asignacionesNuevas = asignacionRepository.findByPeriodo(periodo)
                .stream()
                .collect(Collectors.toMap(a -> a.getAlumno().getId(), a -> a, (a1, a2) -> a1));

        // 🚨 5. Contar a los alumnos que la app móvil ya le asignó a cada maestro para no saturarlos
        Map<Long, Integer> contadorTutorados = new HashMap<>();
        for (AsignacionEntity asig : asignacionesNuevas.values()) {
            contadorTutorados.merge(asig.getDocente().getId(), 1, Integer::sum);
        }

        // 6. Procesar solicitudes pendientes (Ignorará las que ya están en asignacionesNuevas)
        procesarSolicitudes(solicitudes, periodo, contadorTutorados, asignacionesNuevas);

        // 7. Procesar continuidad (alumnos sin solicitud pero con tutor anterior)
        procesarContinuidad(todosLosAlumnos, periodo, asignacionesAnteriores,
                contadorTutorados, asignacionesNuevas);

        // 8. Asignar aleatoriamente a alumnos que se quedaron sin tutor
        asignarAleatoriamente(todosLosAlumnos, periodo, contadorTutorados, asignacionesNuevas);

        log.info("Proceso completado. Total asignaciones en el periodo: {}", asignacionesNuevas.size());
    }
    /**
     * Procesa solicitudes respetando el límite de 13 tutorados por tutor
     */
    private void procesarSolicitudes(
            List<SolicitudTutorEntity> solicitudes,
            String periodo,
            Map<Long, Integer> contadorTutorados,
            Map<Long, AsignacionEntity> asignacionesNuevas) {
        
        for (SolicitudTutorEntity solicitud : solicitudes) {
            Long docenteId = solicitud.getDocente().getId();
            Long alumnoId = solicitud.getAlumno().getId();
            
            // Verificar si ya fue asignado
            if (asignacionesNuevas.containsKey(alumnoId)) {
                continue;
            }
            
            // Verificar límite del docente
            int tutoradosActuales = contadorTutorados.getOrDefault(docenteId, 0);
            int maxTutorados = solicitud.getDocente().getMaxTutorados();
            
            if (tutoradosActuales < maxTutorados) {
                // Asignar
                AsignacionEntity asignacion = crearAsignacion(
                    solicitud.getAlumno(),
                    solicitud.getDocente(),
                    periodo,
                    TipoAsignacion.ELECCION_ALUMNO
                );
                asignacionesNuevas.put(alumnoId, asignacion);
                contadorTutorados.put(docenteId, tutoradosActuales + 1);
                
                log.debug("Asignado por solicitud: alumno {} -> docente {}", 
                    alumnoId, docenteId);
            }
        }
    }

    /**
     * Mantiene la continuidad de alumnos que no solicitaron cambio
     */
    private void procesarContinuidad(
            List<AlumnoEntity> todosLosAlumnos,
            String periodo,
            Map<Long, AsignacionEntity> asignacionesAnteriores,
            Map<Long, Integer> contadorTutorados,
            Map<Long, AsignacionEntity> asignacionesNuevas) {
        
        for (AlumnoEntity alumno : todosLosAlumnos) {
            Long alumnoId = alumno.getId();
            
            // Si ya tiene asignación, continuar
            if (asignacionesNuevas.containsKey(alumnoId)) {
                continue;
            }
            
            // Buscar asignación anterior
            AsignacionEntity asignacionAnterior = asignacionesAnteriores.get(alumnoId);
            if (asignacionAnterior == null) {
                continue; // Es alumno nuevo, se procesará en asignación aleatoria
            }
            
            DocenteEntity tutorAnterior = asignacionAnterior.getDocente();
            Long docenteId = tutorAnterior.getId();
            
            // Verificar si el docente sigue activo y tiene espacio
            if (tutorAnterior.getEstado() == EstadoDocente.ACTIVO){

                int tutoradosActuales = contadorTutorados.getOrDefault(docenteId, 0);
                
                if (tutoradosActuales < tutorAnterior.getMaxTutorados()) {
                    // Mantener continuidad
                    AsignacionEntity asignacion = crearAsignacion(
                        alumno,
                        tutorAnterior,
                        periodo,
                        TipoAsignacion.CONTINUIDAD
                    );
                    asignacionesNuevas.put(alumnoId, asignacion);
                    contadorTutorados.put(docenteId, tutoradosActuales + 1);
                    
                    log.debug("Continuidad mantenida: alumno {} -> docente {}", 
                        alumnoId, docenteId);
                }
            }
        }
    }

    /**
     * Asigna aleatoriamente a alumnos sin tutor
     */
    private void asignarAleatoriamente(
            List<AlumnoEntity> todosLosAlumnos,
            String periodo,
            Map<Long, Integer> contadorTutorados,
            Map<Long, AsignacionEntity> asignacionesNuevas) {
        
        for (AlumnoEntity alumno : todosLosAlumnos) {
            Long alumnoId = alumno.getId();
            
            // Si ya tiene asignación, continuar
            if (asignacionesNuevas.containsKey(alumnoId)) {
                continue;
            }
            
            // Obtener carrera activa del alumno
            Optional<AlumnoMatriculaEntity> matriculaOpt = 
                matriculaRepository.findMatriculaActivaByAlumno(alumnoId);
            
            if (matriculaOpt.isEmpty()) {
                log.warn("Alumno {} sin matrícula activa", alumnoId);
                continue;
            }
            
            Integer carreraId = matriculaOpt.get().getCarrera().getId();
            
            // Intentar asignar en orden de prioridad
            DocenteEntity docenteAsignado = null;
            TipoAsignacion tipoAsignacion = null;
            
            // 1. Intentar asignar de su propia carrera
            docenteAsignado = buscarDocenteDisponible(carreraId, contadorTutorados);
            if (docenteAsignado != null) {
                tipoAsignacion = TipoAsignacion.ALEATORIO_CARRERA;
            }
            
            // 2. Intentar asignar de carreras afines
            if (docenteAsignado == null) {
                List<AfinidadCarrerasEntity> afinidades = 
                    afinidadRepository.findCarrerasAfinesPorPrioridad(carreraId);
                
                for (AfinidadCarrerasEntity afinidad : afinidades) {
                    docenteAsignado = buscarDocenteDisponible(
                        afinidad.getCarreraDestino().getId(), 
                        contadorTutorados
                    );
                    if (docenteAsignado != null) {
                        tipoAsignacion = TipoAsignacion.ALEATORIO_AFIN;
                        break;
                    }
                }
            }
            
            // 3. Asignar de cualquier carrera
            if (docenteAsignado == null) {
                docenteAsignado = buscarCualquierDocenteDisponible(contadorTutorados);
                tipoAsignacion = TipoAsignacion.ALEATORIO_GLOBAL;
            }
            
            if (docenteAsignado != null) {
                AsignacionEntity asignacion = crearAsignacion(
                    alumno,
                    docenteAsignado,
                    periodo,
                    tipoAsignacion
                );
                asignacionesNuevas.put(alumnoId, asignacion);
                contadorTutorados.merge(docenteAsignado.getId(), 1, Integer::sum);
                
                log.debug("Asignación aleatoria {}: alumno {} -> docente {}", 
                    tipoAsignacion, alumnoId, docenteAsignado.getId());
            } else {
                log.error("No se encontró docente disponible para alumno {}", alumnoId);
            }
        }
    }

    /**
     * Busca un docente disponible en una carrera específica
     */
    private DocenteEntity buscarDocenteDisponible(
            Integer carreraId,
            Map<Long, Integer> contadorTutorados) {

        List<DocenteEntity> docentes = docenteRepository
                .findActivosByCarrera(carreraId);
        
        Collections.shuffle(docentes); // Aleatorizar
        
        for (DocenteEntity docente : docentes) {
            int tutoradosActuales = contadorTutorados.getOrDefault(docente.getId(), 0);
            if (tutoradosActuales < docente.getMaxTutorados()) {
                return docente;
            }
        }
        
        return null;
    }

    /**
     * Busca cualquier docente disponible de cualquier carrera
     */
    private DocenteEntity buscarCualquierDocenteDisponible(
            Map<Long, Integer> contadorTutorados) {
        
        List<DocenteEntity> docentes = docenteRepository.findAllActivos();
        Collections.shuffle(docentes);
        
        for (DocenteEntity docente : docentes) {
            int tutoradosActuales = contadorTutorados.getOrDefault(docente.getId(), 0);
            if (tutoradosActuales < docente.getMaxTutorados()) {
                return docente;
            }
        }
        
        return null;
    }

    /**
     * Crea y guarda una nueva asignación
     */
    private AsignacionEntity crearAsignacion(
            AlumnoEntity alumno,
            DocenteEntity docente,
            String periodo,
            TipoAsignacion tipo) {
        
        AsignacionEntity asignacion = new AsignacionEntity();
        asignacion.setAlumno(alumno);
        asignacion.setDocente(docente);
        asignacion.setPeriodo(periodo);
        asignacion.setTipo(tipo);
        
        return asignacionRepository.save(asignacion);
    }

    /**
     * Calcula el periodo anterior (ejemplo: 25-26B -> 25-26A)
     */
    private String calcularPeriodoAnterior(String periodo) {
        if (periodo == null || periodo.isBlank()) return periodo;

        String[] partes = periodo.split("-");
        if (partes.length < 3) {
            log.warn("⚠️ Formato de periodo inesperado en calcularPeriodoAnterior: '{}'", periodo);
            return periodo;
        }

        String ciclo = partes[2].toUpperCase().trim();

        if ("B".equals(ciclo)) {
            // 2025-2026-B → 2025-2026-A (mismo ciclo)
            return partes[0] + "-" + partes[1] + "-A";
        } else {
            // 2025-2026-A → 2024-2025-B (ciclo anterior)
            int anio1 = Integer.parseInt(partes[0]) - 1;
            int anio2 = Integer.parseInt(partes[1]) - 1;
            return anio1 + "-" + anio2 + "-B";
        }
    }

    /**
     * Obtiene el conteo actual de tutorados por docente en un periodo
     */
    public Long contarTutorados(Long docenteId, String periodo) {
        return asignacionRepository.contarTutoradosPorDocenteYPeriodo(docenteId, periodo);
    }

    /**
     * Obtiene todas las asignaciones de un periodo
     */
    public List<AsignacionEntity> obtenerAsignacionesPeriodo(String periodo) {
        return asignacionRepository.findByPeriodo(periodo);
    }
}
