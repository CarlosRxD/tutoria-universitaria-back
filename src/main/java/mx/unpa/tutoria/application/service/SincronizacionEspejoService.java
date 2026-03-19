package mx.unpa.tutoria.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.domain.model.EstadoDocente;
import mx.unpa.tutoria.infrastructure.entity.*;
import mx.unpa.tutoria.infrastructure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de sincronización con el SICE (base de datos "escolares").
 *
 * FLUJO:
 *  SICE (escolares) → Vistas v_sice_* → JdbcTemplate (ReadOnly) → Entidades locales
 *
 * REGLAS DE NEGOCIO:
 *  1. El sice_id (trabajadores.Id_Tra) es la llave dorada de reconciliación de docentes.
 *  2. La matrícula (alumnos.Id_Alu) es la llave dorada de reconciliación de alumnos.
 *  3. Si un docente tiene tieneOverride=true → NO se sobreescribe correo ni estado local.
 *  4. Si el SICE marca Status_Tra con "baja" → SIEMPRE se aplica EstadoDocente.BAJA.
 *  5. Las carreras se reconcilian por codigoInterno (= Id_Car del SICE).
 */
@Slf4j
@Service
//@RequiredArgsConstructor
public class SincronizacionEspejoService {

    // ─── DataSource del SICE (solo lectura) ───────────────────────────────────
    @Qualifier("academicoJdbcTemplate")
    private final JdbcTemplate siceJdbc;

    // ─── Repositorios locales ─────────────────────────────────────────────────
    private final DocenteRepository     docenteRepository;
    private final AlumnoRepository      alumnoRepository;
    private final AlumnoMatriculaRepository matriculaRepository;
    private final CarreraRepository     carreraRepository;

    @Value("${sincronizacion.espejo.enabled:true}")
    private boolean sincronizacionHabilitada;
    @Autowired
    public SincronizacionEspejoService(
            @Qualifier("academicoJdbcTemplate") JdbcTemplate siceJdbc,
            DocenteRepository docenteRepository,
            AlumnoRepository alumnoRepository,
            AlumnoMatriculaRepository matriculaRepository,
            CarreraRepository carreraRepository) {
        this.siceJdbc           = siceJdbc;
        this.docenteRepository  = docenteRepository;
        this.alumnoRepository   = alumnoRepository;
        this.matriculaRepository = matriculaRepository;
        this.carreraRepository  = carreraRepository;
    }
    // =========================================================================
    // SCHEDULER: Cron configurable desde application.properties
    // Por defecto: Domingos a las 23:00
    // =========================================================================
    @Scheduled(cron = "${sincronizacion.espejo.cron:0 0 23 * * SUN}")
    public void sincronizacionAutomatica() {
        if (!sincronizacionHabilitada) {
            log.info("⏸ Sincronización automática deshabilitada por configuración.");
            return;
        }
        log.info("⏰ [SYNC] Iniciando sincronización automática programada...");
        ejecutarSincronizacionEnSegundoPlano();
    }

    @Async
    public void ejecutarSincronizacionEnSegundoPlano() {
        long inicio = System.currentTimeMillis();
        try {
            log.info("🚀 [SYNC] Sincronización iniciada en segundo plano...");
            int docentes = sincronizarDocentes();
            int alumnos  = sincronizarAlumnos();
            long duracion = System.currentTimeMillis() - inicio;
            log.info("✅ [SYNC] Completada: {} docentes, {} alumnos — {}ms", docentes, alumnos, duracion);
        } catch (Exception e) {
            log.error("❌ [SYNC] Error crítico durante sincronización: ", e);
        }
    }

    // =========================================================================
    // SINCRONIZAR DOCENTES
    // Fuente: v_sice_docentes (JOIN trabajadores + profesores + campus)
    //
    // Columnas de la vista:
    //   id_trabajador, nombre_completo, correo, categoria, nivel,
    //   status_sice, fecha_ingreso, fecha_baja, carreras_ids
    // =========================================================================
    @Transactional
    public int sincronizarDocentes() {
        log.info("📥 [SYNC] Leyendo docentes desde v_sice_docentes...");

        List<DocenteSice> sice = siceJdbc.query(
                "SELECT id_trabajador, nombre_completo, correo, categoria, nivel, status_sice, carreras_ids " +
                        "FROM v_sice_docentes",
                (ResultSet rs, int rowNum) -> new DocenteSice(
                        rs.getString("id_trabajador"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo"),
                        rs.getString("categoria"),
                        rs.getString("nivel"),
                        rs.getString("status_sice"),
                        rs.getString("carreras_ids")
                )
        );

        if (sice.isEmpty()) {
            log.warn("⚠️ [SYNC] v_sice_docentes no devolvió registros. Verifica la vista en el SICE.");
            return 0;
        }
        log.info("📊 [SYNC] {} docentes leídos desde el SICE.", sice.size());

        // ── 1. Cargar mapa de entidades existentes por sice_id (batch) ──────
        Set<String> siceIds = sice.stream()
                .map(DocenteSice::idTrabajador)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, DocenteEntity> porSiceId = docenteRepository
                .findAllBySiceIdIn(siceIds).stream()
                .collect(Collectors.toMap(DocenteEntity::getSiceId, d -> d, (a, b) -> a));

        // ── 2. Fallback: mapa por correo (para docentes migrados sin sice_id) ─
        Set<String> correos = sice.stream()
                .map(DocenteSice::correo)
                .filter(this::esCorreoValido)
                .collect(Collectors.toSet());

        Map<String, DocenteEntity> porCorreo = docenteRepository
                .findAllByCorreoIn(correos).stream()
                .filter(d -> d.getSiceId() == null)  // solo los que aún no tienen sice_id
                .collect(Collectors.toMap(DocenteEntity::getCorreo, d -> d, (a, b) -> a));

        // ── 3. Sincronizar carreras referenciadas ────────────────────────────
        Set<String> carreraIds = sice.stream()
                .filter(d -> d.carrerasIds() != null && !d.carrerasIds().isBlank())
                .flatMap(d -> Arrays.stream(d.carrerasIds().split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        Map<String, CarreraEntity> carreraMap = sincronizarCarrerasPorCodigo(carreraIds);

        // ── 4. Procesar cada registro del SICE ──────────────────────────────
        List<DocenteEntity> aGuardar = new ArrayList<>();

        for (DocenteSice d : sice) {
            if (d.idTrabajador() == null || d.idTrabajador().isBlank()) continue;

            // Buscar entidad existente: primero por sice_id, luego por correo (migración)
            DocenteEntity entidad = porSiceId.get(d.idTrabajador());
            if (entidad == null && esCorreoValido(d.correo())) {
                entidad = porCorreo.get(d.correo());
            }
            if (entidad == null) {
                entidad = new DocenteEntity();
            }

            // Siempre actualizar estos campos (no tienen override)
            entidad.setSiceId(d.idTrabajador());
            entidad.setNombreCompleto(limpiar(d.nombreCompleto()));
            entidad.setCategoriaSice(d.categoria());
            entidad.setNivelSice(d.nivel());
            entidad.setUltimaSync(LocalDateTime.now());

            // ── ESCUDO DE CORREO ─────────────────────────────────────────────
            // Si la administradora ya editó el correo (tieneOverride=true), NO lo pisamos.
            if (!entidad.isTieneOverride() && esCorreoValido(d.correo())) {
                entidad.setCorreo(d.correo().trim().toLowerCase());
            }

            // ── ESCUDO DE ESTADO ─────────────────────────────────────────────
            // BAJA del SICE: siempre se aplica, sin importar override.
            // ACTIVO del SICE: solo si el docente no tiene estado local asignado.
            EstadoDocente estadoSice = mapearEstado(d.statusSice());
            if (estadoSice == EstadoDocente.BAJA) {
                entidad.setEstado(EstadoDocente.BAJA);
            } else if (!entidad.isTieneOverride() && entidad.getEstado() == null) {
                entidad.setEstado(EstadoDocente.ACTIVO);
            }

            // ── CARRERA PRINCIPAL ─────────────────────────────────────────────
            // Tomamos la primera carrera del GROUP_CONCAT como carrera principal.
            if (d.carrerasIds() != null && !d.carrerasIds().isBlank()) {
                String primerCarreraId = d.carrerasIds().split(",")[0].trim();
                CarreraEntity carrera = carreraMap.get(primerCarreraId);
                if (carrera != null && entidad.getCarreraPrincipal() == null) {
                    entidad.setCarreraPrincipal(carrera);
                }
            }

            if (entidad.getMaxTutorados() == null) {
                entidad.setMaxTutorados(13);
            }

            aGuardar.add(entidad);
        }

        docenteRepository.saveAll(aGuardar);
        log.info("💾 [SYNC] {} docentes guardados/actualizados.", aGuardar.size());
        return aGuardar.size();
    }

    // =========================================================================
    // SINCRONIZAR ALUMNOS
    // Fuente: v_sice_alumnos_activos
    //
    // Columnas de la vista:
    //   matricula, carrera_id, carrera_nombre, nombre_completo, correo,
    //   status_sice, semestre_actual, ciclo_actual, periodo_actual
    // =========================================================================
    @Transactional
    public int sincronizarAlumnos() {
        log.info("📥 [SYNC] Leyendo alumnos desde v_sice_alumnos_activos...");

        List<AlumnoSice> sice = siceJdbc.query(
                "SELECT matricula, carrera_id, carrera_nombre, nombre_completo, correo, " +
                        "       semestre_actual, ciclo_actual, periodo_actual, status_sice " +
                        "FROM v_sice_alumnos_activos",
                (ResultSet rs, int rowNum) -> new AlumnoSice(
                        rs.getString("matricula"),
                        rs.getString("carrera_id"),
                        rs.getString("carrera_nombre"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo"),
                        rs.getObject("semestre_actual", Integer.class),
                        rs.getString("ciclo_actual"),
                        rs.getString("periodo_actual"),
                        rs.getInt("status_sice")
                )
        );

        if (sice.isEmpty()) {
            log.warn("⚠️ [SYNC] v_sice_alumnos_activos no devolvió registros.");
            return 0;
        }
        log.info("📊 [SYNC] {} alumnos leídos desde el SICE.", sice.size());

        // ── 1. Cargar matriculas existentes en batch ─────────────────────────
        Set<String> matriculas = sice.stream()
                .map(AlumnoSice::matricula)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, AlumnoMatriculaEntity> matriculaMap = matriculaRepository
                .findAllByMatriculaIn(matriculas).stream()
                .collect(Collectors.toMap(AlumnoMatriculaEntity::getMatricula, m -> m, (a, b) -> a));

        // ── 2. Fallback: alumnos por correo (migración desde datos viejos) ───
        Set<String> correos = sice.stream()
                .map(AlumnoSice::correo)
                .filter(this::esCorreoValido)
                .collect(Collectors.toSet());

        Map<String, AlumnoEntity> porCorreo = alumnoRepository
                .findAllByCorreoIn(correos).stream()
                .collect(Collectors.toMap(a -> a.getCorreo().trim(), a -> a, (a, b) -> a));

        // ── 3. Sincronizar carreras ──────────────────────────────────────────
        Set<String> codigosCarrera = sice.stream()
                .map(AlumnoSice::carreraId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, CarreraEntity> carreraMap = sincronizarCarrerasPorCodigo(codigosCarrera);

        // ── 4. Procesar ─────────────────────────────────────────────────────
        // Usamos Map en lugar de List para que si la matrícula viene duplicada
        // en el SICE (bug de datos), se pise a sí misma y no rompa el UNIQUE constraint.
        Map<String, AlumnoEntity>          alumnosGuardar    = new HashMap<>();
        Map<String, AlumnoMatriculaEntity> matriculasGuardar = new HashMap<>();

        for (AlumnoSice a : sice) {
            if (a.matricula() == null || a.matricula().isBlank()) continue;

            AlumnoEntity alumno;
            AlumnoMatriculaEntity matriculaEntidad = matriculaMap.get(a.matricula());

            if (matriculaEntidad != null) {
                // Matrícula ya existe → actualizar el alumno asociado
                alumno = matriculaEntidad.getAlumno();
            } else if (esCorreoValido(a.correo()) && porCorreo.containsKey(a.correo().trim())) {
                // No encontramos la matrícula pero sí el correo → mismo alumno, matrícula nueva
                alumno = porCorreo.get(a.correo().trim());
                matriculaEntidad = new AlumnoMatriculaEntity();
            } else {
                // Registro nuevo
                alumno = alumnosGuardar.computeIfAbsent(a.matricula(), k -> new AlumnoEntity());
                matriculaEntidad = new AlumnoMatriculaEntity();
            }

            // Actualizar datos del alumno
            alumno.setNombreCompleto(limpiar(a.nombreCompleto()));
            if (esCorreoValido(a.correo())) {
                alumno.setCorreo(a.correo().trim().toLowerCase());
            }

            alumnosGuardar.put(a.matricula(), alumno);

            // Actualizar matrícula
            matriculaEntidad.setAlumno(alumno);
            matriculaEntidad.setMatricula(a.matricula());
            matriculaEntidad.setSemestre(a.semestreActual());
            matriculaEntidad.setActiva(a.statusSice() == 1);

            CarreraEntity carrera = carreraMap.get(a.carreraId());
            if (carrera != null) {
                matriculaEntidad.setCarrera(carrera);
            }

            matriculasGuardar.put(a.matricula(), matriculaEntidad);
        }

        alumnoRepository.saveAll(alumnosGuardar.values());
        matriculaRepository.saveAll(matriculasGuardar.values());

        log.info("💾 [SYNC] {} alumnos y {} matrículas guardados/actualizados.",
                alumnosGuardar.size(), matriculasGuardar.size());
        return alumnosGuardar.size();
    }

    // =========================================================================
    // HELPER: Sincronizar carreras por código interno (Id_Car del SICE)
    // Reconciliación: CarreraEntity.codigoInterno = Id_Car del SICE
    // =========================================================================
    private Map<String, CarreraEntity> sincronizarCarrerasPorCodigo(Set<String> codigosRaw) {
        if (codigosRaw == null || codigosRaw.isEmpty()) return new HashMap<>();

        Set<String> codigos = codigosRaw.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());

        // Cargar carreras que ya existen por codigoInterno
        Map<String, CarreraEntity> mapa = carreraRepository
                .findAllByCodigoInternoIn(codigos).stream()
                .collect(Collectors.toMap(c -> c.getCodigoInterno().trim(), c -> c, (a, b) -> a));

        // Consultar nombres desde la vista del SICE para las que no existen
        Set<String> faltantes = codigos.stream()
                .filter(c -> !mapa.containsKey(c))
                .collect(Collectors.toSet());

        if (!faltantes.isEmpty()) {
            // Obtener nombre desde v_sice_carreras para crear las que no existen
            String placeholders = faltantes.stream().map(c -> "?").collect(Collectors.joining(","));
            List<CarreraEntity> nuevas = new ArrayList<>();

            try {
                siceJdbc.query(
                        "SELECT carrera_id, nombre FROM v_sice_carreras WHERE carrera_id IN (" + placeholders + ")",
                        faltantes.toArray(),
                        (ResultSet rs) -> {
                            CarreraEntity c = new CarreraEntity();
                            c.setCodigoInterno(rs.getString("carrera_id"));
                            c.setNombre(rs.getString("nombre"));
                            nuevas.add(c);
                        }
                );
            } catch (Exception e) {
                log.warn("⚠️ [SYNC] No se pudo consultar v_sice_carreras para códigos faltantes: {}", e.getMessage());
                // Fallback: crear con nombre = código
                faltantes.forEach(cod -> {
                    CarreraEntity c = new CarreraEntity();
                    c.setCodigoInterno(cod);
                    c.setNombre(cod);
                    nuevas.add(c);
                });
            }

            if (!nuevas.isEmpty()) {
                List<CarreraEntity> guardadas = carreraRepository.saveAll(nuevas);
                guardadas.forEach(c -> mapa.put(c.getCodigoInterno().trim(), c));
                log.info("🆕 [SYNC] {} carreras nuevas creadas.", guardadas.size());
            }
        }

        return mapa;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private EstadoDocente mapearEstado(String estado) {
        if (estado == null) return EstadoDocente.ACTIVO;
        String e = estado.toUpperCase().trim();
        if (e.contains("BAJA"))     return EstadoDocente.BAJA;
        if (e.contains("SABATICO")) return EstadoDocente.SABATICO;
        if (e.contains("INHABIL"))  return EstadoDocente.INHABIL;
        return EstadoDocente.ACTIVO;
    }

    private boolean esCorreoValido(String correo) {
        if (correo == null || correo.isBlank()) return false;
        String c = correo.trim();
        return c.contains("@") && c.length() > 5 && !c.contains("@actualizar.com");
    }

    private String limpiar(String valor) {
        if (valor == null) return "";
        // Normaliza espacios múltiples y trim
        return valor.trim().replaceAll("\\s{2,}", " ");
    }

    // =========================================================================
    // RECORDS INTERNOS — Mapeo de las vistas del SICE
    // =========================================================================

    /** Columnas de v_sice_docentes */
    record DocenteSice(
            String idTrabajador,
            String nombreCompleto,
            String correo,
            String categoria,
            String nivel,
            String statusSice,
            String carrerasIds
    ) {}

    /** Columnas de v_sice_alumnos_activos */
    record AlumnoSice(
            String  matricula,
            String  carreraId,
            String  carreraNombre,
            String  nombreCompleto,
            String  correo,
            Integer semestreActual,
            String  cicloActual,
            String  periodoActual,
            int     statusSice
    ) {}

    /** Resultado devuelto al controller */
    public record ResultadoSincronizacion(
            int docentesActualizados,
            int alumnosActualizados,
            LocalDateTime timestamp
    ) {}
}