package mx.unpa.tutoria.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.infrastructure.entity.DocenteEntity;
import mx.unpa.tutoria.infrastructure.repository.DocenteRepository;
import mx.unpa.tutoria.infrastructure.repository.AsignacionRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final OficioPdfService oficioPdfService;
    private final DocenteRepository docenteRepository;
    private final AsignacionRepository asignacionRepository;
    private final ConstanciaPdfService constanciaPdfService;

    /**
     * Envía oficios a todos los docentes con tutorados en un periodo
     * Ejecuta de forma ASÍNCRONA
     */
    @Async
    public CompletableFuture<Integer> enviarOficiosATodosLosDocentes(String periodo) {
        log.info("🚀 Iniciando envío masivo de oficios para periodo: {}", periodo);

        List<DocenteEntity> docentes = docenteRepository.findAllActivos();
        int enviosExitosos = 0;

        for (DocenteEntity docente : docentes) {
            try {
                // Verificar si tiene tutorados
                Long cantidadTutorados = asignacionRepository
                        .contarTutoradosPorDocenteYPeriodo(docente.getId(), periodo);

                if (cantidadTutorados > 0) {
                    enviarOficioADocenteSincrono(docente.getId(), periodo);
                    enviosExitosos++;
                    log.info("✅ Oficio enviado a: {}", docente.getCorreo());
                }
            } catch (Exception e) {
                log.error("❌ Error enviando oficio a {}: {}",
                        docente.getCorreo(), e.getMessage());
            }
        }

        log.info("🎉 Total de oficios enviados: {}", enviosExitosos);
        return CompletableFuture.completedFuture(enviosExitosos);
    }

    /**
     * Envía el oficio a un docente específico de forma ASÍNCRONA
     * ✅ NUEVO: Retorna inmediatamente, el correo se envía en background
     */
    @Async
    public CompletableFuture<Void> enviarOficioADocente(Long docenteId, String periodo) {
        log.info("📤 Iniciando envío asíncrono de oficio: docente={}, periodo={}", docenteId, periodo);

        try {
            enviarOficioADocenteSincrono(docenteId, periodo);
            log.info("✅ Oficio enviado exitosamente en background");
        } catch (Exception e) {
            log.error("❌ Error en envío asíncrono: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Método privado que hace el envío real del correo
     * (No expuesto públicamente, usado internamente)
     */
    private void enviarOficioADocenteSincrono(Long docenteId, String periodo) {
        DocenteEntity docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado: " + docenteId));

        log.info("📧 Generando PDF para docente: {}", docente.getNombreCompleto());

        // Generar PDF
        byte[] pdfBytes = oficioPdfService.generarOficioTutorados(docenteId, periodo);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(docente.getCorreo());
            helper.setSubject("Asignación de Tutorados - Periodo " + periodo);

            String cuerpoHtml = construirCuerpoCorreo(docente.getNombreCompleto(), periodo);
            helper.setText(cuerpoHtml, true);

            // Adjuntar PDF
            String nombreArchivo = String.format("Oficio_Tutorados_%s_%s.pdf",
                    docente.getNombreCompleto().replace(" ", "_"), periodo);

            helper.addAttachment(nombreArchivo, new ByteArrayResource(pdfBytes));

            log.info("📮 Enviando correo a: {}", docente.getCorreo());
            mailSender.send(message);
            log.info("✅ Correo enviado exitosamente a: {}", docente.getCorreo());

        } catch (MessagingException e) {
            log.error("❌ Error enviando correo a {}: {}", docente.getCorreo(), e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo", e);
        }
    }

    private String construirCuerpoCorreo(String nombreDocente, String periodo) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color: #2c3e50;">Asignación de Tutorados - Periodo %s</h2>
                
                <p>Estimado(a) <strong>%s</strong>,</p>
                
                <p>Por medio del presente correo, le hacemos llegar el oficio con la lista 
                de estudiantes asignados bajo su tutoría para el periodo académico <strong>%s</strong>.</p>
                
                <p>Le solicitamos establecer contacto con cada uno de sus tutorados durante 
                las primeras dos semanas del semestre para programar sus sesiones de tutoría.</p>
                
                <p>En el archivo adjunto encontrará el oficio oficial con toda la información.</p>
                
                <p>Si tiene alguna duda o comentario, no dude en contactarnos.</p>
                
                <br>
                <p><strong>Atentamente,</strong></p>
                <p>Coordinación de Tutorías<br>
                Universidad del Papaloapan</p>
                
                <hr style="border: 1px solid #ddd;">
                <p style="font-size: 12px; color: #7f8c8d;">
                Este es un correo automático, por favor no responda a esta dirección.
                </p>
            </body>
            </html>
            """, periodo, nombreDocente, periodo);
    }
    @Async
    public CompletableFuture<Integer> enviarConstanciasATodosLosDocentes(String periodo) {
        log.info("🚀 Iniciando envío masivo de CONSTANCIAS para periodo: {}", periodo);

        List<DocenteEntity> docentes = docenteRepository.findAllActivos();
        int enviosExitosos = 0;

        for (DocenteEntity docente : docentes) {
            try {
                // Verificar si tiene tutorados
                Long cantidadTutorados = asignacionRepository
                        .contarTutoradosPorDocenteYPeriodo(docente.getId(), periodo);

                if (cantidadTutorados > 0) {
                    enviarConstanciaADocenteSincrono(docente.getId(), periodo);
                    enviosExitosos++;
                    log.info("✅ Constancia enviada a: {}", docente.getCorreo());
                }
            } catch (Exception e) {
                log.error("❌ Error enviando constancia a {}: {}",
                        docente.getCorreo(), e.getMessage());
            }
        }

        log.info("🎉 Total de constancias enviadas: {}", enviosExitosos);
        return CompletableFuture.completedFuture(enviosExitosos);
    }

    /**
     * Envía constancia a un docente específico de forma ASÍNCRONA
     */
    @Async
    public CompletableFuture<Void> enviarConstanciaADocente(Long docenteId, String periodo) {
        log.info("📤 Iniciando envío asíncrono de CONSTANCIA: docente={}, periodo={}",
                docenteId, periodo);

        try {
            enviarConstanciaADocenteSincrono(docenteId, periodo);
            log.info("✅ Constancia enviada exitosamente en background");
        } catch (Exception e) {
            log.error("❌ Error en envío asíncrono de constancia: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Método privado que hace el envío real de la constancia
     */
    private void enviarConstanciaADocenteSincrono(Long docenteId, String periodo) {
        DocenteEntity docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado: " + docenteId));

        log.info("📧 Generando CONSTANCIA PDF para docente: {}", docente.getNombreCompleto());

        // Generar PDF de CONSTANCIA (no oficio)
        byte[] pdfBytes = constanciaPdfService.generarConstanciaParticipacion(docenteId, periodo);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(docente.getCorreo());
            helper.setSubject("CONSTANCIA DEL INFORME SEMESTRAL DE TUTORÍAS " + periodo);

            String cuerpoHtml = construirCuerpoCorreoConstancia(docente.getNombreCompleto(), periodo);
            helper.setText(cuerpoHtml, true);

            // Adjuntar PDF
            String nombreArchivo = String.format("Constancia_Participacion_%s_%s.pdf",
                    docente.getNombreCompleto().replace(" ", "_"), periodo);

            helper.addAttachment(nombreArchivo, new ByteArrayResource(pdfBytes));

            log.info("📮 Enviando constancia a: {}", docente.getCorreo());
            mailSender.send(message);
            log.info("✅ Constancia enviada exitosamente a: {}", docente.getCorreo());

        } catch (MessagingException e) {
            log.error("❌ Error enviando constancia a {}: {}",
                    docente.getCorreo(), e.getMessage(), e);
            throw new RuntimeException("Error al enviar constancia", e);
        }
    }

    /**
     * Construye el cuerpo HTML del correo de CONSTANCIA
     * (Diferente al correo de asignación inicial)
     */
    private String construirCuerpoCorreoConstancia(String nombreDocente, String periodo) {
        return String.format("""
        <html>
        <body style="font-family: Arial, sans-serif;">
            <h2 style="color: #2c3e50;">CONSTANCIA DEL INFORME SEMESTRAL DE TUTORÍAS %s</h2>
            
            <p>Estimable <strong>%s</strong>,</p>
            
            <p>Por medio del presente nos permitimos enviarle un cordial saludo a nombre 
            de los integrantes del <strong>Comité Institucional de Tutorías</strong>; así mismo, 
            confirmamos la recepción de su evaluación "Informe Semestral de Tutorías %s" y 
            adjuntamos en respuesta el oficio <strong>"Constancia de participación"</strong> 
            avalado por CIT UNPA Campus Loma Bonita.</p>
            
            <p>Sin otro en particular, les agradecemos el apoyo en este ejercicio %s.</p>
            
            <br>
            <p><strong>Comité Institucional de Tutorías</strong><br>
            Universidad del Papaloapan<br>
            Campus Loma Bonita</p>
            
            <hr style="border: 1px solid #ddd;">
            <p style="font-size: 12px; color: #7f8c8d;">
            Este es un correo automático, por favor no responda a esta dirección.
            </p>
        </body>
        </html>
        """, periodo, nombreDocente, periodo, periodo);
    }
}