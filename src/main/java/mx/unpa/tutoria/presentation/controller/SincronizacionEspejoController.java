package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.application.service.SincronizacionEspejoService;
import mx.unpa.tutoria.application.service.SincronizacionEspejoService.ResultadoSincronizacion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sincronizacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SincronizacionEspejoController {

    private final SincronizacionEspejoService sincronizacionService;

    /**
     * Sincronización FULL: docentes + alumnos desde el SICE.
     * POST /api/sincronizacion/espejo
     */
    @PostMapping("/espejo")
    public ResponseEntity<ResultadoSincronizacion> sincronizarEspejo() {
        try {
            log.info("📡 [CTRL] Petición de sincronización FULL recibida.");

            int docentes = sincronizacionService.sincronizarDocentes();
            int alumnos  = sincronizacionService.sincronizarAlumnos();

            log.info("✅ [CTRL] FULL completada: {} docentes, {} alumnos.", docentes, alumnos);
            return ResponseEntity.ok(new ResultadoSincronizacion(docentes, alumnos, LocalDateTime.now()));

        } catch (Exception e) {
            log.error("❌ [CTRL] Error en sincronización FULL: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sincronizar solo docentes (útil cuando solo cambió el personal).
     * POST /api/sincronizacion/docentes
     */
    @PostMapping("/docentes")
    public ResponseEntity<Map<String, Object>> sincronizarDocentes() {
        try {
            log.info("📡 [CTRL] Sincronización solo DOCENTES solicitada.");
            int total = sincronizacionService.sincronizarDocentes();
            return ResponseEntity.ok(Map.of(
                    "docentes", total,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("❌ [CTRL] Error al sincronizar docentes: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sincronizar solo alumnos (es la más frecuente — cada reinscripción).
     * POST /api/sincronizacion/alumnos
     */
    @PostMapping("/alumnos")
    public ResponseEntity<Map<String, Object>> sincronizarAlumnos() {
        try {
            log.info("📡 [CTRL] Sincronización solo ALUMNOS solicitada.");
            int total = sincronizacionService.sincronizarAlumnos();
            return ResponseEntity.ok(Map.of(
                    "alumnos", total,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("❌ [CTRL] Error al sincronizar alumnos: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Estado actual de la sincronización.
     * GET /api/sincronizacion/espejo/estado
     */
    @GetMapping("/espejo/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        return ResponseEntity.ok(Map.of(
                "habilitado",          true,
                "cron",                "Domingos 23:00",
                "vistas_sice",         new String[]{"v_sice_docentes", "v_sice_alumnos_activos", "v_sice_carreras"},
                "timestamp_consulta",  LocalDateTime.now().toString()
        ));
    }
}