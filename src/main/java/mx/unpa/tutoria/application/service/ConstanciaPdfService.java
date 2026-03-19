package mx.unpa.tutoria.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.infrastructure.entity.AlumnoEntity;
import mx.unpa.tutoria.infrastructure.entity.AsignacionEntity;
import mx.unpa.tutoria.infrastructure.entity.AlumnoMatriculaEntity;
import mx.unpa.tutoria.infrastructure.repository.AlumnoMatriculaRepository;
import mx.unpa.tutoria.infrastructure.repository.AsignacionRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servicio para generar PDFs de CONSTANCIA de Participación
 * ✅ HOMOLOGADO: Firma centrada y tabla optimizada con fuente 8.5 e interlineado de 9.5.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConstanciaPdfService {

    private final AsignacionRepository asignacionRepository;
    private final AlumnoMatriculaRepository matriculaRepository;

    public byte[] generarConstanciaParticipacion(Long docenteId, String periodo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Márgenes laterales de 50 y verticales de 30
            Document document = new Document(PageSize.LETTER, 50, 50, 30, 30);
            PdfWriter.getInstance(document, baos);

            // Tamaños de fuente profesionales
            Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font fontSubtitulo = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font fontBold = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font fontNormal = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font fontItalic = new Font(Font.HELVETICA, 10, Font.ITALIC);

            // ✅ Fuentes exclusivas para la tabla homologadas al oficio
            Font fontTableHeader = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font fontTableSmall = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Font fontSmall = new Font(Font.HELVETICA, 8, Font.NORMAL);
            Font fontDireccion = new Font(Font.HELVETICA, 8, Font.NORMAL);

            Phrase footerPhrase = new Phrase();
            footerPhrase.add(new Chunk("c.c.p Comité Institucional de Tutorías.\n", fontSmall));
            footerPhrase.add(new Chunk("________________________________________________________________________________________________________\n", fontDireccion));
            footerPhrase.add(new Chunk("Universidad del Papaloapan, Av. Ferrocarril S/N, Cd. Universitaria. Loma Bonita Oax., C. P. 68400, Tel: 01 (281) 87 2 92 30", fontDireccion));

            HeaderFooter footer = new HeaderFooter(footerPhrase, false);
            footer.setBorder(Rectangle.NO_BORDER);
            footer.setAlignment(Element.ALIGN_LEFT);
            document.setFooter(footer);

            document.open();

            // 1. Encabezado
            agregarEncabezadoEquilibrado(document, fontTitulo, fontSubtitulo);

            // 2. Asunto
            Paragraph asunto = new Paragraph("Asunto: Constancia de Participación.", fontNormal);
            asunto.setAlignment(Element.ALIGN_RIGHT);
            document.add(asunto);
            document.add(new Paragraph(" \n", fontSmall));

            // 3. Datos del Docente
            List<AsignacionEntity> asignaciones = asignacionRepository.findTutoradosConDetalles(docenteId, periodo);
            String nombreDocente = asignaciones.isEmpty()
                    ? "DOCENTE SIN ASIGNACIONES"
                    : asignaciones.get(0).getDocente().getNombreCompleto();

            // 4. Cuerpo de la constancia
            String[] fechasPeriodo = calcularFechasPeriodo(periodo);

            Paragraph cuerpo = new Paragraph(
                    String.format(
                            "Por este medio se hace constar que el(la) %s es Profesor(a)-Investigador(a) y Tutor(a) en el " +
                                    "Programa Institucional de Tutorías, en la modalidad personalizada y ha cumplido " +
                                    "satisfactoriamente el proceso de acompañamiento dentro del ámbito universitario y " +
                                    "académico en esta Universidad, durante el periodo %s que comprende del %s al %s. " +
                                    "Teniendo a su cargo los siguientes alumnos:",
                            nombreDocente.toUpperCase(),
                            periodo,
                            fechasPeriodo[0],
                            fechasPeriodo[1]
                    ),
                    fontNormal
            );
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            cuerpo.setLeading(14f);
            document.add(cuerpo);

            document.add(new Paragraph(" \n", fontSmall));

            // 5. Tabla de Tutorados
            if (!asignaciones.isEmpty()) {
                // Pasamos las fuentes exclusivas de la tabla
                agregarTablaTutoradosEquilibrada(document, asignaciones, fontTableHeader, fontTableSmall);
            } else {
                document.add(new Paragraph("No hay alumnos asignados en este periodo.", fontItalic));
            }

            document.add(new Paragraph(" \n", fontSmall));

            // 6. Pie de constancia
            LocalDate hoy = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "MX"));

            Paragraph piedoc = new Paragraph(
                    String.format(
                            "A petición del interesado y para los fines y usos legales que al mismo convengan, " +
                                    "se extiende la presente en la Ciudad de Loma Bonita, Oaxaca, a %s.",
                            hoy.format(formatter)
                    ),
                    fontNormal
            );
            piedoc.setAlignment(Element.ALIGN_JUSTIFIED);
            piedoc.setLeading(14f);
            document.add(piedoc);

            document.add(new Paragraph(" \n", fontSmall));

            // 7. Zona de Firmas
            agregarZonaFirmasEquilibrada(document, fontBold, fontNormal, fontItalic);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando constancia PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar constancia PDF", e);
        }
    }

    private String[] calcularFechasPeriodo(String periodo) {
        // Formato esperado: "2025-2026-A" o "2025-2026-B"
        // split("-") → ["2025", "2026", "A"]
        String[] partes = periodo.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "dd 'de' MMMM 'de' yyyy", new Locale("es", "MX"));

        if (partes.length >= 3) {
            // Formato correcto: 2025-2026-A
            int anio1 = Integer.parseInt(partes[0]);
            int anio2 = Integer.parseInt(partes[1]);
            String ciclo = partes[2].toUpperCase().trim();

            if ("A".equals(ciclo)) {
                // Semestre A: octubre anio1 → febrero anio2
                LocalDate inicio = LocalDate.of(anio1, 10, 1);
                LocalDate fin    = LocalDate.of(anio2, 2,  9);
                return new String[]{ inicio.format(formatter), fin.format(formatter) };
            } else {
                // Semestre B: febrero anio2 → julio anio2
                LocalDate inicio = LocalDate.of(anio2, 2,  10);
                LocalDate fin    = LocalDate.of(anio2, 7,  31);
                return new String[]{ inicio.format(formatter), fin.format(formatter) };
            }
        }

        // Fallback si el formato es inesperado — retorna año actual
        LocalDate hoy = LocalDate.now();
        String hoyStr = hoy.format(formatter);
        log.warn("⚠️ Formato de periodo inesperado: '{}'. Usando fecha actual como fallback.", periodo);
        return new String[]{ hoyStr, hoyStr };
    }




    private void agregarEncabezadoEquilibrado(Document document, Font fontTitulo, Font fontSubtitulo) {
        try {
            Table headerTable = new Table(3);
            headerTable.setWidth(100);
            headerTable.setWidths(new float[]{20f, 60f, 20f});
            headerTable.setBorderWidth(0);
            headerTable.setPadding(0);

            Cell cellLogo = new Cell();
            cellLogo.setBorder(0);
            cellLogo.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);

            URL logoUnpaUrl = getClass().getResource("/static/images/logo_unpa.png");
            if (logoUnpaUrl != null) {
                Image logoUnpa = Image.getInstance(logoUnpaUrl);
                logoUnpa.scaleToFit(75, 75);
                cellLogo.addElement(logoUnpa);
            }
            headerTable.addCell(cellLogo);

            Cell cellTexto = new Cell();
            cellTexto.setBorder(0);
            cellTexto.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph titulo = new Paragraph("UNIVERSIDAD DEL PAPALOAPAN\n", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);

            Paragraph subtitulo = new Paragraph("Comité Institucional de Tutorías Campus Loma Bonita", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);

            cellTexto.addElement(titulo);
            cellTexto.addElement(subtitulo);
            headerTable.addCell(cellTexto);

            Cell cellVacia = new Cell();
            cellVacia.setBorder(0);
            headerTable.addCell(cellVacia);

            document.add(headerTable);

            Paragraph linea = new Paragraph("_".repeat(85), new Font(Font.HELVETICA, 8));
            linea.setAlignment(Element.ALIGN_CENTER);
            document.add(linea);
            document.add(new Paragraph(" \n", new Font(Font.HELVETICA, 6)));

        } catch (Exception e) {
            log.warn("Error armando el encabezado: " + e.getMessage());
        }
    }

    private void agregarZonaFirmasEquilibrada(Document document, Font fontBold, Font fontNormal, Font fontItalic) {
        try {
            Table firmasTable = new Table(3);
            firmasTable.setWidth(100);
            firmasTable.setWidths(new float[]{15f, 60f, 25f});
            firmasTable.setBorderWidth(0);

            Cell cellIzq = new Cell();
            cellIzq.setBorder(0);
            firmasTable.addCell(cellIzq);

            Cell cellCentro = new Cell();
            cellCentro.setBorder(0);
            cellCentro.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph atentamente = new Paragraph("Atentamente\n", fontNormal);
            atentamente.setAlignment(Element.ALIGN_CENTER);
            atentamente.setLeading(11f);
            cellCentro.addElement(atentamente);

            Paragraph lema1 = new Paragraph("Terra ubérrima, mens aperta", fontItalic);
            lema1.setAlignment(Element.ALIGN_CENTER);
            lema1.setLeading(11f);
            cellCentro.addElement(lema1);

            Paragraph lema2 = new Paragraph("Bou lo-tama, chí jí jú\n", fontItalic);
            lema2.setAlignment(Element.ALIGN_CENTER);
            lema2.setLeading(11f);
            cellCentro.addElement(lema2);

            URL firmaUrl = getClass().getResource("/static/images/firma.png");
            if (firmaUrl != null) {
                Image imgFirma = Image.getInstance(firmaUrl);
                imgFirma.scaleToFit(120, 60);

                Paragraph pFirma = new Paragraph();
                pFirma.setAlignment(Element.ALIGN_CENTER);
                pFirma.add(new Chunk(imgFirma, 0, 0, true));

                cellCentro.addElement(pFirma);
            } else {
                cellCentro.addElement(new Paragraph("\n\n"));
            }

            Paragraph nombreFirma = new Paragraph("Dra. Carolina Gabriela Maldonado Méndez", fontBold);
            nombreFirma.setAlignment(Element.ALIGN_CENTER);
            nombreFirma.setLeading(11f);
            cellCentro.addElement(nombreFirma);

            Paragraph cargoFirma = new Paragraph("Representante del Comité Institucional de Tutorías", fontNormal);
            cargoFirma.setAlignment(Element.ALIGN_CENTER);
            cargoFirma.setLeading(11f);
            cellCentro.addElement(cargoFirma);

            firmasTable.addCell(cellCentro);

            Cell cellDer = new Cell();
            cellDer.setBorder(0);
            cellDer.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellDer.setVerticalAlignment(Element.ALIGN_BOTTOM);

            URL logoComiteUrl = getClass().getResource("/static/images/logo_comite.png");
            if (logoComiteUrl != null) {
                Image imgComite = Image.getInstance(logoComiteUrl);
                imgComite.scaleToFit(80, 80);
                imgComite.setAlignment(Element.ALIGN_LEFT);
                cellDer.addElement(imgComite);
            }
            firmasTable.addCell(cellDer);

            document.add(firmasTable);

        } catch (Exception e) {
            log.warn("Error armando la zona de firmas: " + e.getMessage());
        }
    }

    private void agregarTablaTutoradosEquilibrada(Document document, List<AsignacionEntity> asignaciones,
                                                  Font fontTableHeader, Font fontTableSmall) throws DocumentException {
        Table tabla = new Table(3);
        tabla.setWidth(100);
        tabla.setWidths(new float[]{45f, 45f, 10f});
        tabla.setPadding(1.5f); // Reducimos un poco el padding interno
        tabla.setSpacing(0);

        Color gris = new Color(230, 230, 230);

        Cell headerTutorado = new Cell(new Paragraph("Tutorado", fontTableHeader));
        headerTutorado.setHeader(true);
        headerTutorado.setBackgroundColor(gris);
        tabla.addCell(headerTutorado);

        Cell headerCarrera = new Cell(new Paragraph("Carrera", fontTableHeader));
        headerCarrera.setHeader(true);
        headerCarrera.setBackgroundColor(gris);
        tabla.addCell(headerCarrera);

        Cell headerSemestre = new Cell(new Paragraph("Sem.", fontTableHeader));
        headerSemestre.setHeader(true);
        headerSemestre.setBackgroundColor(gris);
        tabla.addCell(headerSemestre);

        tabla.endHeaders();

        for (AsignacionEntity asignacion : asignaciones) {
            AlumnoEntity alumno = asignacion.getAlumno();
            Optional<AlumnoMatriculaEntity> matriculaOpt = matriculaRepository
                    .findMatriculaActivaByAlumno(alumno.getId());

            String carrera = matriculaOpt
                    .map(m -> m.getCarrera() != null ? m.getCarrera().getNombre() : "N/A")
                    .orElse("N/A");
            String semestre = matriculaOpt
                    .map(m -> m.getSemestre() != null ? String.valueOf(m.getSemestre()) : "N/A")
                    .orElse("N/A");

            // ✅ Se aplica el interlineado de 9.5f para el tamaño 8.5
            Paragraph pNombre = new Paragraph(alumno.getNombreCompleto(), fontTableSmall);
            pNombre.setLeading(10);
            tabla.addCell(new Cell(pNombre));

            Paragraph pCarrera = new Paragraph(carrera, fontTableSmall);
            pCarrera.setLeading(10);
            tabla.addCell(new Cell(pCarrera));

            Paragraph pSemestre = new Paragraph(semestre, fontTableSmall);
            pSemestre.setLeading(10);
            Cell cellSemestre = new Cell(pSemestre);
            cellSemestre.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSemestre.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(cellSemestre);
        }
        document.add(tabla);
    }
}