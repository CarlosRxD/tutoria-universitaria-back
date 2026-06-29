package mx.unpa.tutoria.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.domain.model.EstadoAsignacion;
import mx.unpa.tutoria.domain.model.EstadoDocente;
import mx.unpa.tutoria.domain.model.TipoAsignacion;
import mx.unpa.tutoria.infrastructure.entity.*;
import mx.unpa.tutoria.infrastructure.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionTutorService {

    private final AsignacionRepository        asignacionRepository;
    private final SolicitudTutorRepository    solicitudRepository;
    private final AlumnoRepository            alumnoRepository;
    private final DocenteRepository           docenteRepository;
    private final AlumnoMatriculaRepository   matriculaRepository;
    private final AfinidadCarrerasRepository  afinidadRepository;

    // =========================================================================
    //  PROCESO PRINCIPAL
    //
    //  Optimización N+1 → Batch:
    //    Antes : O(N × M) queries  (una por alumno × niveles de afinidad)
    //    Ahora : ~6 queries totales + 1 saveAll al final
    // =========================================================================
    @Transactional
    public void procesarAsignacionesPeriodo(String periodo) {
        log.info("Iniciando proceso de asignación para periodo: {}", periodo);

        // ── PRE-CARGA ─────────────────────────────────────────────────────────
        // 1 query por tipo; los loops ya no tocan la BD

        List<AlumnoEntity> todosLosAlumnos = alumnoRepository.findAll();

        // Docentes activos agrupados por carrera para lookup O(1)
        List<DocenteEntity> todosDocentes = docenteRepository.findAllActivos();
        Map<Integer, List<DocenteEntity>> docentesPorCarrera = todosDocentes.stream()
                .collect(Collectors.groupingBy(d -> d.getCarreraPrincipal().getId()));

        // Afinidades ordenadas por prioridad, agrupadas por carrera origen
        Map<Integer, List<Integer>> afinidadesPorCarrera = afinidadRepository.findAll().stream()
                .sorted(Comparator.comparing(AfinidadCarrerasEntity::getNivelPrioridad))
                .collect(Collectors.groupingBy(
                        a -> a.getCarreraOrigen().getId(),
                        Collectors.mapping(a -> a.getCarreraDestino().getId(), Collectors.toList())
                ));

        // Matrícula activa por alumno → carrera
        Map<Long, Integer> carreraPorAlumno = matriculaRepository.findAllActivas().stream()
                .collect(Collectors.toMap(
                        m -> m.getAlumno().getId(),
                        m -> m.getCarrera().getId(),
                        (m1, m2) -> m1          // ante duplicado conservar el primero
                ));

        // Solicitudes del periodo
        List<SolicitudTutorEntity> solicitudes =
                solicitudRepository.findByPeriodoOrdenadoPorFecha(periodo);

        // Asignaciones del periodo anterior (para continuidad)
        String periodoAnterior = calcularPeriodoAnterior(periodo);
        Map<Long, AsignacionEntity> asignacionesAnteriores =
                asignacionRepository.findByPeriodo(periodoAnterior).stream()
                        .collect(Collectors.toMap(
                                a -> a.getAlumno().getId(), a -> a, (a1, a2) -> a1));

        // Asignaciones ya existentes en este periodo (hechas desde la app móvil)
        Map<Long, AsignacionEntity> asignacionesNuevas =
                asignacionRepository.findByPeriodo(periodo).stream()
                        .collect(Collectors.toMap(
                                a -> a.getAlumno().getId(), a -> a, (a1, a2) -> a1));

        // Contador de tutorados actuales por docente
        Map<Long, Integer> contadorTutorados = new HashMap<>();
        for (AsignacionEntity asig : asignacionesNuevas.values()) {
            contadorTutorados.merge(asig.getDocente().getId(), 1, Integer::sum);
        }

        // ── PROCESO EN MEMORIA ────────────────────────────────────────────────
        // Las entidades se acumulan en esta lista; ningún método llama save()
        List<AsignacionEntity> nuevasParaGuardar = new ArrayList<>();

        procesarSolicitudes(solicitudes, periodo,
                contadorTutorados, asignacionesNuevas, nuevasParaGuardar);

        procesarContinuidad(todosLosAlumnos, periodo, asignacionesAnteriores,
                contadorTutorados, asignacionesNuevas, nuevasParaGuardar);

        asignarAleatoriamente(todosLosAlumnos, periodo,
                contadorTutorados, asignacionesNuevas, nuevasParaGuardar,
                docentesPorCarrera, todosDocentes, afinidadesPorCarrera, carreraPorAlumno);

        // ── GUARDADO BATCH ────────────────────────────────────────────────────
        // Un único INSERT colectivo en lugar de N individuales
        if (!nuevasParaGuardar.isEmpty()) {
            asignacionRepository.saveAll(nuevasParaGuardar);
            log.info("Guardadas {} nuevas asignaciones en batch", nuevasParaGuardar.size());
        }

        log.info("Proceso completado. Total asignaciones en el periodo: {}",
                asignacionesNuevas.size());
    }

    // =========================================================================
    //  Paso 1 — Solicitudes explícitas del alumno
    // =========================================================================
    private void procesarSolicitudes(
            List<SolicitudTutorEntity> solicitudes,
            String periodo,
            Map<Long, Integer>          contadorTutorados,
            Map<Long, AsignacionEntity> asignacionesNuevas,
            List<AsignacionEntity>      nuevasParaGuardar) {

        for (SolicitudTutorEntity solicitud : solicitudes) {
            Long docenteId = solicitud.getDocente().getId();
            Long alumnoId  = solicitud.getAlumno().getId();

            if (asignacionesNuevas.containsKey(alumnoId)) continue;

            int tutoradosActuales = contadorTutorados.getOrDefault(docenteId, 0);
            int maxTutorados      = solicitud.getDocente().getMaxTutorados();

            if (tutoradosActuales < maxTutorados) {
                AsignacionEntity asignacion = crearAsignacion(
                        solicitud.getAlumno(), solicitud.getDocente(), periodo,
                        TipoAsignacion.ELECCION_ALUMNO, EstadoAsignacion.ASIGNADO);

                asignacionesNuevas.put(alumnoId, asignacion);
                nuevasParaGuardar.add(asignacion);
                contadorTutorados.put(docenteId, tutoradosActuales + 1);

                log.debug("Solicitud: alumno {} → docente {}", alumnoId, docenteId);
            }
        }
    }

    // =========================================================================
    //  Paso 2 — Continuidad con el tutor del periodo anterior
    // =========================================================================
    private void procesarContinuidad(
            List<AlumnoEntity>          todosLosAlumnos,
            String                      periodo,
            Map<Long, AsignacionEntity> asignacionesAnteriores,
            Map<Long, Integer>          contadorTutorados,
            Map<Long, AsignacionEntity> asignacionesNuevas,
            List<AsignacionEntity>      nuevasParaGuardar) {

        for (AlumnoEntity alumno : todosLosAlumnos) {
            Long alumnoId = alumno.getId();
            if (asignacionesNuevas.containsKey(alumnoId)) continue;

            AsignacionEntity anterior = asignacionesAnteriores.get(alumnoId);
            if (anterior == null) continue;

            DocenteEntity tutorAnterior = anterior.getDocente();
            Long docenteId = tutorAnterior.getId();

            if (tutorAnterior.getEstado() == EstadoDocente.ACTIVO) {
                int tutoradosActuales = contadorTutorados.getOrDefault(docenteId, 0);

                if (tutoradosActuales < tutorAnterior.getMaxTutorados()) {
                    AsignacionEntity asignacion = crearAsignacion(
                            alumno, tutorAnterior, periodo,
                            TipoAsignacion.CONTINUIDAD, EstadoAsignacion.ASIGNADO);

                    asignacionesNuevas.put(alumnoId, asignacion);
                    nuevasParaGuardar.add(asignacion);
                    contadorTutorados.put(docenteId, tutoradosActuales + 1);

                    log.debug("Continuidad: alumno {} → docente {}", alumnoId, docenteId);
                }
            }
        }
    }

    // =========================================================================
    //  Paso 3 — Asignación aleatoria por niveles (todo en memoria, sin queries)
    // =========================================================================
    private void asignarAleatoriamente(
            List<AlumnoEntity>              todosLosAlumnos,
            String                          periodo,
            Map<Long, Integer>              contadorTutorados,
            Map<Long, AsignacionEntity>     asignacionesNuevas,
            List<AsignacionEntity>          nuevasParaGuardar,
            Map<Integer, List<DocenteEntity>> docentesPorCarrera,
            List<DocenteEntity>             todosDocentes,
            Map<Integer, List<Integer>>     afinidadesPorCarrera,
            Map<Long, Integer>              carreraPorAlumno) {

        for (AlumnoEntity alumno : todosLosAlumnos) {
            Long alumnoId = alumno.getId();
            if (asignacionesNuevas.containsKey(alumnoId)) continue;

            Integer carreraId = carreraPorAlumno.get(alumnoId);
            if (carreraId == null) {
                log.warn("Alumno {} sin matrícula activa — omitido", alumnoId);
                continue;
            }

            DocenteEntity docenteAsignado = null;
            TipoAsignacion tipo = null;

            // Tier 1: misma carrera
            docenteAsignado = buscarDocenteDisponibleEnLista(
                    docentesPorCarrera.getOrDefault(carreraId, List.of()),
                    contadorTutorados);
            if (docenteAsignado != null) tipo = TipoAsignacion.ALEATORIO_CARRERA;

            // Tier 2: carreras afines en orden de prioridad
            if (docenteAsignado == null) {
                for (Integer carreraAfinId :
                        afinidadesPorCarrera.getOrDefault(carreraId, List.of())) {
                    docenteAsignado = buscarDocenteDisponibleEnLista(
                            docentesPorCarrera.getOrDefault(carreraAfinId, List.of()),
                            contadorTutorados);
                    if (docenteAsignado != null) {
                        tipo = TipoAsignacion.ALEATORIO_AFIN;
                        break;
                    }
                }
            }

            // Tier 3: cualquier docente activo
            if (docenteAsignado == null) {
                docenteAsignado = buscarDocenteDisponibleEnLista(todosDocentes, contadorTutorados);
                if (docenteAsignado != null) tipo = TipoAsignacion.ALEATORIO_GLOBAL;
            }

            if (docenteAsignado != null) {
                AsignacionEntity asignacion = crearAsignacion(
                        alumno, docenteAsignado, periodo, tipo, EstadoAsignacion.ASIGNADO);

                asignacionesNuevas.put(alumnoId, asignacion);
                nuevasParaGuardar.add(asignacion);
                contadorTutorados.merge(docenteAsignado.getId(), 1, Integer::sum);

                log.debug("Aleatorio {}: alumno {} → docente {}", tipo, alumnoId, docenteAsignado.getId());
            } else {
                log.error("Sin docente disponible para alumno {}", alumnoId);
            }
        }
    }

    // =========================================================================
    //  Helpers privados
    // =========================================================================

    /**
     * Busca el primer docente con capacidad disponible dentro de una lista
     * ya cargada en memoria. Baraja la lista para distribuir la carga uniformemente.
     */
    private DocenteEntity buscarDocenteDisponibleEnLista(
            List<DocenteEntity>  docentes,
            Map<Long, Integer>   contadorTutorados) {

        List<DocenteEntity> shuffled = new ArrayList<>(docentes);
        Collections.shuffle(shuffled);

        for (DocenteEntity docente : shuffled) {
            if (contadorTutorados.getOrDefault(docente.getId(), 0) < docente.getMaxTutorados())
                return docente;
        }
        return null;
    }

    /**
     * Construye la entidad sin persistirla.
     * El guardado ocurre en batch al final del proceso principal.
     *
     * @throws NullPointerException imposible: tipo y estado son parámetros requeridos.
     */
    private AsignacionEntity crearAsignacion(
            AlumnoEntity    alumno,
            DocenteEntity   docente,
            String          periodo,
            TipoAsignacion  tipo,
            EstadoAsignacion estado) {

        AsignacionEntity asignacion = new AsignacionEntity();
        asignacion.setAlumno(alumno);
        asignacion.setDocente(docente);
        asignacion.setPeriodo(periodo);
        asignacion.setTipo(tipo);
        asignacion.setEstado(estado);
        asignacion.setFechaAsignacion(LocalDateTime.now());
        return asignacion;  // ← sin save(); el caller agrega a nuevasParaGuardar
    }

    /**
     * Calcula el periodo académico inmediatamente anterior.
     *   2025-2026-B  →  2025-2026-A
     *   2025-2026-A  →  2024-2025-B
     */
    private String calcularPeriodoAnterior(String periodo) {
        if (periodo == null || periodo.isBlank()) return periodo;

        String[] partes = periodo.split("-");
        if (partes.length < 3) {
            log.warn("Formato de periodo inesperado en calcularPeriodoAnterior: '{}'", periodo);
            return periodo;
        }

        if ("B".equalsIgnoreCase(partes[2].trim())) {
            return partes[0] + "-" + partes[1] + "-A";
        } else {
            int anio1 = Integer.parseInt(partes[0]) - 1;
            int anio2 = Integer.parseInt(partes[1]) - 1;
            return anio1 + "-" + anio2 + "-B";
        }
    }

    // =========================================================================
    //  API pública auxiliar
    // =========================================================================

    public Long contarTutorados(Long docenteId, String periodo) {
        return asignacionRepository.contarTutoradosPorDocenteYPeriodo(docenteId, periodo);
    }

    public List<AsignacionEntity> obtenerAsignacionesPeriodo(String periodo) {
        return asignacionRepository.findByPeriodo(periodo);
    }
}
