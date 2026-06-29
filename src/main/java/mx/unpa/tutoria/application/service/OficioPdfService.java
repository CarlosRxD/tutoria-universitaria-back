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
 * Servicio para generar PDFs de OFICIO de Asignación.
 *
 * ✅ ADAPTIVE LAYOUT: Ajusta dinámicamente márgenes, fuentes y espaciados
 * según el número de alumnos para garantizar que SIEMPRE quepa en 1 hoja.
 *
 * Capacidad estimada por nivel:
 *   Nivel 1 (≤8  alumnos): Layout cómodo, fuentes normales.
 *   Nivel 2 (≤12 alumnos): Layout comprimido moderadamente.
 *   Nivel 3 (≤18 alumnos): Layout máximo, todo reducido.
 *   Nivel 4 (>18 alumnos): Layout extremo (casos excepcionales).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OficioPdfService {

    private final AsignacionRepository asignacionRepository;
    private final AlumnoMatriculaRepository matriculaRepository;

    // =========================================================
    // CONFIG RECORD — Todos los parámetros de layout en un solo lugar
    // =========================================================

    /**
     * Configuración de layout adaptativo.
     *
     * @param margenVertical   Margen superior e inferior del documento (pt)
     * @param espaciadorPt     Tamaño de fuente de los párrafos separadores " \n"
     * @param fontTablaHeader  Tamaño de fuente del encabezado de la tabla
     * @param fontTablaContent Tamaño de fuente del contenido de la tabla
     * @param tablaPadding     Padding interno de cada celda (pt)
     * @param tablaLeading     Interlineado dentro de cada celda (pt)
     * @param fontCuerpo       Tamaño de fuente del cuerpo del texto
     * @param leadingCuerpo    Interlineado del cuerpo del texto (pt)
     */
    private record DocConfig(
            float margenVertical,
            float espaciadorPt,
            float fontTablaHeader,
            float fontTablaContent,
            float tablaPadding,
            float tablaLeading,
            float fontCuerpo,
            float leadingCuerpo
    ) {}

    /**
     * Calcula la configuración óptima según la cantidad de alumnos.
     *
     * Rango real del sistema: 1–13 alumnos (maxTutorados = 13).
     * El peor caso es 13 alumnos con carreras largas como
     * "LIC EN MEDICINA VETERINARIA Y ZOOTECNIA" que generan wrap de 2 líneas.
     *
     *  Nivel 1 (1–7):   Layout cómodo, fuentes y márgenes originales.
     *  Nivel 2 (8–11):  Compresión moderada, cabe bien con carreras mixtas.
     *  Nivel 3 (12–13): Compresión máxima, diseñado para el peor caso real.
     */
    private DocConfig calcularConfig(int numAlumnos) {
        // Orden de parámetros asumido:
        // (margenV, espaciador, fTabHead, fTabCont, padding, leading, fCuerpo, leadCuerpo)

        if (numAlumnos <= 7) {
            // Nivel 1 — Súper holgado (Aumentamos la letra base a 11 y tabla a 9.5)
            return new DocConfig(30f,    6f,      10.0f,    9.5f,    2.0f,    10.5f,   11f,     13f);

        } else if (numAlumnos <= 10) {
            // Nivel 2 — Ajuste intermedio (Letra base de 10.5 y tabla de 9.0)
            return new DocConfig(20f,    4f,      9.5f,     9.0f,    1.5f,    10.0f,   10.5f,   12f);

        } else if (numAlumnos <= 13) {
            // Nivel 3 — El reto real (Casos de 13 alumnos con carreras largas como Veterinaria)
            // Reducimos drásticamente los márgenes (12f) y el espaciador (2f)
            // para mantener una fuente muy digna de 8.5f en la tabla y 10f en el cuerpo.
            return new DocConfig(12f,    2f,      9.0f,     8.5f,    1.0f,    9.5f,    10f,     11f);

        } else {
            // Nivel 4 — Emergencia (14 a 16 alumnos)
            // La fuente mínima para impresión aceptable es 8pt.
            // El padding de 0.5f y margen de 10f exprimen cada milímetro de la hoja.
            return new DocConfig(10f,    1f,      8.5f,     8.0f,    0.5f,    9.0f,    9.5f,    10.5f);
        }
    }

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    public byte[] generarOficioTutorados(Long docenteId, String periodo) {
        try {
            // ── PASO 1: Consultar datos ANTES de crear el Document ──────────
            // Necesitamos saber cuántos alumnos hay para calcular el layout correcto.
            List<AsignacionEntity> asignaciones =
                    asignacionRepository.findTutoradosConDetalles(docenteId, periodo);

            int numAlumnos = asignaciones.size();
            DocConfig cfg = calcularConfig(numAlumnos);

            log.debug("Generando oficio: docente={}, periodo={}, alumnos={}, nivel={}",
                    docenteId, periodo, numAlumnos, cfg);

            // ── PASO 2: Crear documento con márgenes adaptados ──────────────
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(
                    PageSize.LETTER,
                    50, 50,                          // márgenes laterales fijos
                    cfg.margenVertical(),            // margen superior dinámico
                    cfg.margenVertical()             // margen inferior dinámico
            );
            PdfWriter.getInstance(document, baos);

            // ── PASO 3: Fuentes ─────────────────────────────────────────────
            Font fontTitulo    = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font fontSubtitulo = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font fontBold      = new Font(Font.HELVETICA, cfg.fontCuerpo(), Font.BOLD);
            Font fontNormal    = new Font(Font.HELVETICA, cfg.fontCuerpo(), Font.NORMAL);
            Font fontItalic    = new Font(Font.HELVETICA, cfg.fontCuerpo(), Font.ITALIC);
            Font fontSmall     = new Font(Font.HELVETICA, 8, Font.NORMAL);
            Font fontDireccion = new Font(Font.HELVETICA, 8, Font.NORMAL);

            Font fontTablaHeader  = new Font(Font.HELVETICA, cfg.fontTablaHeader(),  Font.BOLD);
            Font fontTablaContent = new Font(Font.HELVETICA, cfg.fontTablaContent(), Font.NORMAL);

            // ── PASO 4: Footer ──────────────────────────────────────────────
            Phrase footerPhrase = new Phrase();
            footerPhrase.add(new Chunk("Este documento no es una constancia.\n", fontSmall));
            footerPhrase.add(new Chunk("c.c.p Comité Institucional de Tutorías.\n", fontSmall));
            footerPhrase.add(new Chunk(
                    "________________________________________________________________________________________________________\n",
                    fontDireccion));
            footerPhrase.add(new Chunk(
                    "Universidad del Papaloapan, Av. Ferrocarril S/N, Cd. Universitaria. "
                            + "Loma Bonita Oax., C. P. 68400, Tel: 01 (281) 87 2 92 30",
                    fontDireccion));

            HeaderFooter footer = new HeaderFooter(footerPhrase, false);
            footer.setBorder(Rectangle.NO_BORDER);
            footer.setAlignment(Element.ALIGN_LEFT);
            document.setFooter(footer);

            document.open();

            // ── PASO 5: Espaciador reutilizable ─────────────────────────────
            // Crea un párrafo separador del tamaño correcto para este layout.
            Font fontEspaciador = new Font(Font.HELVETICA, cfg.espaciadorPt());

            // ── PASO 6: Contenido ───────────────────────────────────────────

            // 1. Encabezado
            agregarEncabezadoEquilibrado(document, fontTitulo, fontSubtitulo);

            // 2. Fecha
            LocalDate hoy = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
            Paragraph fecha = new Paragraph(
                    "Loma Bonita, Oaxaca, a " + hoy.format(formatter) + ".", fontNormal);
            fecha.setAlignment(Element.ALIGN_RIGHT);
            fecha.setLeading(cfg.leadingCuerpo());
            document.add(fecha);

            // 3. Asunto
            Paragraph asunto = new Paragraph("Asunto: Carta de Asignación.", fontNormal);
            asunto.setAlignment(Element.ALIGN_RIGHT);
            asunto.setLeading(cfg.leadingCuerpo());
            document.add(asunto);
            document.add(new Paragraph(" \n", fontEspaciador));

            // 4. Datos del docente
            String nombreDocente = asignaciones.isEmpty()
                    ? "DOCENTE SIN ASIGNACIONES"
                    : asignaciones.get(0).getDocente().getNombreCompleto();

            Paragraph destinatario = new Paragraph(nombreDocente, fontBold);
            destinatario.setLeading(cfg.leadingCuerpo());
            document.add(destinatario);

            Paragraph cargo = new Paragraph("Profesor(a)-Investigador(a)", fontNormal);
            cargo.setLeading(cfg.leadingCuerpo());
            document.add(cargo);

            Paragraph presente = new Paragraph("Presente", fontNormal);
            presente.setLeading(cfg.leadingCuerpo());
            document.add(presente);
            document.add(new Paragraph(" \n", fontEspaciador));

            // 5. Cuerpo
            Paragraph cuerpo = new Paragraph(
                    "A nombre del Comité Institucional de Tutorías, me es grato darle a conocer "
                            + "el listado de alumnos que lo han seleccionado o ratificado como su tutor "
                            + "para el semestre " + periodo + ".",
                    fontNormal);
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            cuerpo.setLeading(cfg.leadingCuerpo());
            document.add(cuerpo);
            document.add(new Paragraph(" \n", fontEspaciador));

            // 6. Tabla
            if (!asignaciones.isEmpty()) {
                agregarTablaTutorados(document, asignaciones, fontTablaHeader, fontTablaContent, cfg);
            } else {
                document.add(new Paragraph("No hay alumnos asignados en este periodo.", fontItalic));
            }
            document.add(new Paragraph(" \n", fontEspaciador));

            // 7. Despedida
            Paragraph despedida = new Paragraph(
                    "De antemano le deseamos un buen desarrollo de la tutoría y el Comité "
                            + "Institucional de Tutorías queda a sus órdenes para aclarar dudas o comentarios.",
                    fontNormal);
            despedida.setAlignment(Element.ALIGN_JUSTIFIED);
            despedida.setLeading(cfg.leadingCuerpo());
            document.add(despedida);
            document.add(new Paragraph(" \n", fontEspaciador));

            // 8. Zona de firmas
            agregarZonaFirmasEquilibrada(document, fontBold, fontNormal, fontItalic);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar oficio PDF", e);
        }
    }

    // =========================================================
    // TABLA DE TUTORADOS — recibe config para layout adaptativo
    // =========================================================

    private void agregarTablaTutorados(
            Document document,
            List<AsignacionEntity> asignaciones,
            Font fontTablaHeader,
            Font fontTablaContent,
            DocConfig cfg) throws DocumentException {

        Table tabla = new Table(4);
        tabla.setWidth(100);
        tabla.setWidths(new float[]{28f, 26f, 8f, 38f});
        tabla.setPadding(cfg.tablaPadding());
        tabla.setSpacing(0);

        Color gris = new Color(230, 230, 230);

        // Encabezados
        Cell hTutorado = new Cell(new Paragraph("Tutorado", fontTablaHeader));
        hTutorado.setHeader(true);
        hTutorado.setBackgroundColor(gris);
        tabla.addCell(hTutorado);

        Cell hCarrera = new Cell(new Paragraph("Carrera", fontTablaHeader));
        hCarrera.setHeader(true);
        hCarrera.setBackgroundColor(gris);
        tabla.addCell(hCarrera);

        Cell hSemestre = new Cell(new Paragraph("Sem.", fontTablaHeader));
        hSemestre.setHeader(true);
        hSemestre.setBackgroundColor(gris);
        tabla.addCell(hSemestre);

        Cell hCorreo = new Cell(new Paragraph("Correo", fontTablaHeader));
        hCorreo.setHeader(true);
        hCorreo.setBackgroundColor(gris);
        tabla.addCell(hCorreo);

        tabla.endHeaders();

        // Filas de datos
        for (AsignacionEntity asignacion : asignaciones) {
            AlumnoEntity alumno = asignacion.getAlumno();
            Optional<AlumnoMatriculaEntity> matriculaOpt =
                    matriculaRepository.findMatriculaActivaByAlumno(alumno.getId());

            String carrera  = matriculaOpt
                    .map(m -> m.getCarrera() != null ? m.getCarrera().getNombre() : "N/A")
                    .orElse("N/A");
            String semestre = matriculaOpt
                    .map(m -> m.getSemestre() != null ? String.valueOf(m.getSemestre()) : "N/A")
                    .orElse("N/A");
            String correo   = alumno.getCorreo() != null ? alumno.getCorreo() : "N/A";

            Paragraph pNombre = new Paragraph(alumno.getNombreCompleto(), fontTablaContent);
            pNombre.setLeading(cfg.tablaLeading());
            tabla.addCell(new Cell(pNombre));

            Paragraph pCarrera = new Paragraph(carrera, fontTablaContent);
            pCarrera.setLeading(cfg.tablaLeading());
            tabla.addCell(new Cell(pCarrera));

            Paragraph pSemestre = new Paragraph(semestre, fontTablaContent);
            pSemestre.setLeading(cfg.tablaLeading());
            Cell cellSem = new Cell(pSemestre);
            cellSem.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSem.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(cellSem);

            Paragraph pCorreo = new Paragraph(correo, fontTablaContent);
            pCorreo.setLeading(cfg.tablaLeading());
            tabla.addCell(new Cell(pCorreo));
        }

        document.add(tabla);
    }

    // =========================================================
    // ENCABEZADO
    // =========================================================

    private void agregarEncabezadoEquilibrado(
            Document document, Font fontTitulo, Font fontSubtitulo) {
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

            Paragraph subtitulo = new Paragraph(
                    "Comité Institucional de Tutorías Campus Loma Bonita", fontSubtitulo);
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
            log.warn("Error armando el encabezado: {}", e.getMessage());
        }
    }

    // =========================================================
    // ZONA DE FIRMAS
    // =========================================================

    private void agregarZonaFirmasEquilibrada(
            Document document, Font fontBold, Font fontNormal, Font fontItalic) {
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
                imgFirma.scaleToFit(110, 50);
                Paragraph pFirma = new Paragraph();
                pFirma.setAlignment(Element.ALIGN_CENTER);
                pFirma.add(new Chunk(imgFirma, 0, 0, true));
                cellCentro.addElement(pFirma);
            } else {
                cellCentro.addElement(new Paragraph("\n\n"));
            }

            Paragraph nombreFirma = new Paragraph(
                    "Dra. Carolina Gabriela Maldonado Méndez", fontBold);
            nombreFirma.setAlignment(Element.ALIGN_CENTER);
            nombreFirma.setLeading(11f);
            cellCentro.addElement(nombreFirma);

            Paragraph cargoFirma = new Paragraph(
                    "Representante del Comité Institucional de Tutorías", fontNormal);
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
            log.warn("Error armando la zona de firmas: {}", e.getMessage());
        }
    }

    // =========================================================
    // UTILIDAD — Fechas del periodo (mantenida para compatibilidad)
    // =========================================================

    private String[] calcularFechasPeriodo(String periodo) {
        String[] partes = periodo.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "dd 'de' MMMM 'de' yyyy", new Locale("es", "MX"));

        if (partes.length >= 3) {
            int anio1   = Integer.parseInt(partes[0]);
            int anio2   = Integer.parseInt(partes[1]);
            String ciclo = partes[2].toUpperCase().trim();

            if ("A".equals(ciclo)) {
                LocalDate inicio = LocalDate.of(anio1, 10, 1);
                LocalDate fin    = LocalDate.of(anio2,  2, 9);
                return new String[]{ inicio.format(formatter), fin.format(formatter) };
            } else {
                LocalDate inicio = LocalDate.of(anio2, 2, 10);
                LocalDate fin    = LocalDate.of(anio2, 7, 31);
                return new String[]{ inicio.format(formatter), fin.format(formatter) };
            }
        }

        LocalDate hoy    = LocalDate.now();
        String    hoyStr = hoy.format(formatter);
        log.warn("⚠️ Formato de periodo inesperado: '{}'. Usando fecha actual como fallback.", periodo);
        return new String[]{ hoyStr, hoyStr };
    }
}