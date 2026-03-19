package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.application.service.ConstanciaPdfService;
import mx.unpa.tutoria.application.service.EmailService;
import mx.unpa.tutoria.application.service.ExcelImportService;
import mx.unpa.tutoria.application.service.OficioPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final ExcelImportService excelImportService;
    private final OficioPdfService oficioPdfService;
    private final EmailService emailService;
    private final ConstanciaPdfService constanciaPdfService;

    /**
     * Importa docentes desde un archivo Excel
     */
    @PostMapping("/importar/docentes")
    public ResponseEntity<String> importarDocentes(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío");
            }

            int registros = excelImportService.importarDocentes(file);
            return ResponseEntity.ok(
                    String.format("Se importaron %d docentes exitosamente", registros)
            );

        } catch (Exception e) {
            log.error("Error importando docentes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error al importar docentes: " + e.getMessage());
        }
    }

    /**
     * Importa alumnos desde un archivo Excel
     */
    @PostMapping("/importar/alumnos")
    public ResponseEntity<String> importarAlumnos(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío");
            }

            int registros = excelImportService.importarAlumnos(file);
            return ResponseEntity.ok(
                    String.format("Se importaron %d alumnos exitosamente", registros)
            );

        } catch (Exception e) {
            log.error("Error importando alumnos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error al importar alumnos: " + e.getMessage());
        }
    }

    /**
     * Genera el PDF de oficio para un docente
     */
    @GetMapping("/oficio/docente/{docenteId}/periodo/{periodo}")
    public ResponseEntity<byte[]> generarOficio(
            @PathVariable Long docenteId,
            @PathVariable String periodo) {

        try {
            byte[] pdfBytes = oficioPdfService.generarOficioTutorados(docenteId, periodo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    String.format("Oficio_Tutorados_%s.pdf", periodo));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generando oficio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Envía oficios por correo a todos los docentes de un periodo
     * ⚠️ EJECUTA EN BACKGROUND - Retorna respuesta inmediata
     */
    @PostMapping("/enviar-oficios/periodo/{periodo}")
    public ResponseEntity<?> enviarOficios(@PathVariable String periodo) {
        try {
            log.info("📥 Solicitud de envío masivo recibida para periodo: {}", periodo);

            // Inicia el proceso en background
            emailService.enviarOficiosATodosLosDocentes(periodo);

            // Retorna inmediatamente
            return ResponseEntity.ok(Map.of(
                    "mensaje", "El envío de oficios ha sido iniciado. Los correos se están enviando en segundo plano.",
                    "periodo", periodo,
                    "nota", "Recibirá los correos en los próximos minutos"
            ));

        } catch (Exception e) {
            log.error("Error iniciando envío de oficios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Error al iniciar el envío de oficios",
                            "detalle", e.getMessage()
                    ));
        }
    }

    /**
     * Envía oficio a un docente específico
     * ✅ NUEVO: Retorna respuesta INMEDIATA, correo se envía en background
     */
    @PostMapping("/enviar-oficio/docente/{docenteId}/periodo/{periodo}")
    public ResponseEntity<?> enviarOficioDocente(
            @PathVariable Long docenteId,
            @PathVariable String periodo) {

        try {
            log.info("📥 Solicitud de envío individual: docente={}, periodo={}", docenteId, periodo);

            // ✅ Inicia el envío en background (método asíncrono)
            emailService.enviarOficioADocente(docenteId, periodo);

            // ✅ Retorna INMEDIATAMENTE (sin esperar a que el correo se envíe)
            log.info("✅ Respuesta enviada al frontend, correo procesándose en background");

            return ResponseEntity.ok(Map.of(
                    "mensaje", "El oficio está siendo enviado. El correo llegará en breve.",
                    "docenteId", docenteId,
                    "periodo", periodo,
                    "estado", "procesando"
            ));

        } catch (Exception e) {
            log.error("❌ Error iniciando envío de oficio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Error al iniciar el envío del oficio",
                            "detalle", e.getMessage()
                    ));
        }
    }
    @GetMapping("/constancia/docente/{docenteId}/periodo/{periodo}")
    public ResponseEntity<byte[]> generarConstancia(
            @PathVariable Long docenteId,
            @PathVariable String periodo) {

        try {
            byte[] pdfBytes = constanciaPdfService.generarConstanciaParticipacion(docenteId, periodo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    String.format("Constancia_Participacion_%s.pdf", periodo));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generando constancia: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Envía constancias de participación a todos los docentes del periodo
     * SE USA AL FINAL DEL SEMESTRE (después de recibir informes)
     * ⚠️ EJECUTA EN BACKGROUND - Retorna respuesta inmediata
     */
    @PostMapping("/enviar-constancias/periodo/{periodo}")
    public ResponseEntity<?> enviarConstancias(@PathVariable String periodo) {
        try {
            log.info("📥 Solicitud de envío masivo de CONSTANCIAS para periodo: {}", periodo);

            // Inicia el proceso en background
            emailService.enviarConstanciasATodosLosDocentes(periodo);

            // Retorna inmediatamente
            return ResponseEntity.ok(Map.of(
                    "mensaje", "El envío de constancias ha sido iniciado. Los correos se están enviando en segundo plano.",
                    "periodo", periodo,
                    "nota", "Las constancias llegarán en los próximos minutos"
            ));

        } catch (Exception e) {
            log.error("Error iniciando envío de constancias: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Error al iniciar el envío de constancias",
                            "detalle", e.getMessage()
                    ));
        }
    }

    /**
     * Envía constancia a un docente específico
     * ✅ NUEVO: Retorna respuesta INMEDIATA, correo se envía en background
     */
    @PostMapping("/enviar-constancia/docente/{docenteId}/periodo/{periodo}")
    public ResponseEntity<?> enviarConstanciaDocente(
            @PathVariable Long docenteId,
            @PathVariable String periodo) {

        try {
            log.info("📥 Solicitud de envío individual de CONSTANCIA: docente={}, periodo={}",
                    docenteId, periodo);

            // ✅ Inicia el envío en background (método asíncrono)
            emailService.enviarConstanciaADocente(docenteId, periodo);

            // ✅ Retorna INMEDIATAMENTE (sin esperar a que el correo se envíe)
            log.info("✅ Respuesta enviada al frontend, constancia procesándose en background");

            return ResponseEntity.ok(Map.of(
                    "mensaje", "La constancia está siendo enviada. El correo llegará en breve.",
                    "docenteId", docenteId,
                    "periodo", periodo,
                    "estado", "procesando"
            ));

        } catch (Exception e) {
            log.error("❌ Error iniciando envío de constancia: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Error al iniciar el envío de la constancia",
                            "detalle", e.getMessage()
                    ));
        }
    }
}