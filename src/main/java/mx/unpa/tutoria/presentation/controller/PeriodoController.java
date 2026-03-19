package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador de periodos escolares.
 *
 * Lee DIRECTAMENTE del SICE (escolares.periodosescolares) vía siceJdbc.
 * Convierte el formato nativo del SICE (Id_Cic_FK='2526', Id_Per='A')
 * al formato legible de la app ('2025-2026-A').
 *
 * Conversión: '2526' → '20'+'25' + '-' + '20'+'26' = '2025-2026'
 */
@Slf4j
@RestController
@RequestMapping("/api/periodos")
@RequiredArgsConstructor
public class PeriodoController {

    @Qualifier("academicoJdbcTemplate")
    private final JdbcTemplate siceJdbc;

    /**
     * Retorna todos los periodos escolares del SICE en formato '2025-2026-A'.
     * Los componentes Angular usan esta lista para el selector de periodo.
     */
    @GetMapping
    public ResponseEntity<List<String>> obtenerTodosLosPeriodos() {
        try {
            List<String> periodos = siceJdbc.queryForList(
                    """
                    SELECT CONCAT('20', SUBSTRING(Id_Cic_FK, 1, 2), '-20', SUBSTRING(Id_Cic_FK, 3, 2), '-', Id_Per)
                    FROM periodosescolares
                    WHERE Id_Per IN ('A', 'B')
                    ORDER BY Fechainicio_Per DESC
                    LIMIT 12
                    """,
                    String.class
            );

            if (periodos.isEmpty()) {
                // Fallback si el SICE no tiene datos — periodos más recientes conocidos
                return ResponseEntity.ok(List.of("2025-2026-B", "2025-2026-A", "2024-2025-B"));
            }

            return ResponseEntity.ok(periodos);

        } catch (Exception e) {
            log.error("❌ [Periodos] Error leyendo periodos del SICE: {}", e.getMessage());
            return ResponseEntity.ok(List.of("2025-2026-B", "2025-2026-A", "2024-2025-B"));
        }
    }

    /**
     * Retorna el periodo activo actual basado en la fecha del sistema.
     * Busca el periodo cuyas fechas envuelven la fecha actual.
     */
    @GetMapping("/activo")
    public ResponseEntity<Map<String, String>> obtenerPeriodoActivo() {
        try {
            // Primero: buscar periodo que cubra hoy
            List<String> activos = siceJdbc.queryForList(
                    """
                    SELECT CONCAT('20', SUBSTRING(Id_Cic_FK,1,2), '-20', SUBSTRING(Id_Cic_FK,3,2), '-', Id_Per)
                    FROM periodosescolares
                    WHERE Id_Per IN ('A','B')
                      AND Fechainicio_Per <= CURDATE()
                      AND Fechafin_Per    >= CURDATE()
                    ORDER BY Fechainicio_Per DESC
                    LIMIT 1
                    """, String.class);

            if (!activos.isEmpty()) {
                log.info("📅 [Periodos] Periodo activo: {}", activos.get(0));
                return ResponseEntity.ok(Map.of("periodo", activos.get(0)));
            }

            // Fallback: el más reciente aunque no cubra hoy
            List<String> recientes = siceJdbc.queryForList(
                    """
                    SELECT CONCAT('20', SUBSTRING(Id_Cic_FK,1,2), '-20', SUBSTRING(Id_Cic_FK,3,2), '-', Id_Per)
                    FROM periodosescolares
                    WHERE Id_Per IN ('A','B')
                    ORDER BY Fechainicio_Per DESC
                    LIMIT 1
                    """, String.class);

            String periodo = recientes.isEmpty() ? "2025-2026-B" : recientes.get(0);
            log.info("📅 [Periodos] Sin periodo activo hoy, usando más reciente: {}", periodo);
            return ResponseEntity.ok(Map.of("periodo", periodo));

        } catch (Exception e) {
            log.error("❌ [Periodos] Error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("periodo", "2025-2026-B"));
        }
    }
}