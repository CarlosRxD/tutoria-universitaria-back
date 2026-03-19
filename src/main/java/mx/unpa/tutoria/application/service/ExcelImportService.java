package mx.unpa.tutoria.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.domain.model.EstadoDocente;
import mx.unpa.tutoria.infrastructure.entity.*;
import mx.unpa.tutoria.infrastructure.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final DocenteRepository docenteRepository;
    private final AlumnoRepository alumnoRepository;
    private final CarreraRepository carreraRepository;
    private final AlumnoMatriculaRepository matriculaRepository;

    // =========================================================
    // IMPORTACIÓN DE DOCENTES
    // =========================================================

    @Transactional
    public int importarDocentes(MultipartFile file) throws IOException {
        // ... (Tu código de docentes puede quedarse igual o optimizarse similar,
        // pero por ahora centrémonos en Alumnos que es donde tenías la duda)
        // Si quieres optimizar docentes también, avísame.
        // Por brevedad dejo la versión simple aquí, pero idealmente aplica la misma lógica.

        int registrosImportados = 0;
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String nombre = getCellValueAsString(row.getCell(0));
                    String correo = getCellValueAsString(row.getCell(1));
                    String estadoStr = getCellValueAsString(row.getCell(2));
                    String carreraNombre = getCellValueAsString(row.getCell(3));

                    if (nombre == null || nombre.isBlank() || correo == null || correo.isBlank()) continue;

                    DocenteEntity docente = docenteRepository.findByCorreo(correo).orElse(new DocenteEntity());
                    docente.setNombreCompleto(nombre.trim());
                    docente.setCorreo(correo.trim().toLowerCase());

                    if (estadoStr != null && !estadoStr.isBlank()) {
                        try {
                            docente.setEstado(EstadoDocente.valueOf(estadoStr.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            docente.setEstado(EstadoDocente.ACTIVO);
                        }
                    }

                    if (carreraNombre != null && !carreraNombre.isBlank()) {
                        CarreraEntity carrera = buscarOCrearCarreraIndividual(carreraNombre);
                        docente.setCarreraPrincipal(carrera);
                    }
                    docenteRepository.save(docente);
                    registrosImportados++;
                } catch (Exception e) {
                    log.error("Error fila docente {}: {}", i, e.getMessage());
                }
            }
        }
        return registrosImportados;
    }

    // =========================================================
    // IMPORTACIÓN DE ALUMNOS (OPTIMIZADO / BATCH)
    // =========================================================

    @Transactional
    public int importarAlumnos(MultipartFile file) throws IOException {
        List<AlumnoRow> filas = new ArrayList<>();

        // 1. LEER EXCEL EN MEMORIA
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nombre = getCellValueAsString(row.getCell(0));
                String correo = getCellValueAsString(row.getCell(1));
                String matricula = getCellValueAsString(row.getCell(2));
                String carrera = getCellValueAsString(row.getCell(3));
                Integer semestre = getCellValueAsInteger(row.getCell(4));

                if (nombre == null || nombre.isBlank() || matricula == null || matricula.isBlank()) {
                    continue;
                }

                filas.add(new AlumnoRow(
                        nombre.trim(),
                        correo != null ? correo.trim().toLowerCase() : null,
                        matricula.trim(),
                        carrera != null ? carrera.trim() : null,
                        semestre
                ));
            }
        }

        if (filas.isEmpty()) return 0;

        // 2. PREPARAR SETS PARA CONSULTAS MASIVAS
        Set<String> matriculas = filas.stream().map(AlumnoRow::matricula).collect(Collectors.toSet());

        Set<String> correos = filas.stream()
                .map(AlumnoRow::correo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> nombresCarreras = filas.stream()
                .map(AlumnoRow::carrera)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. CARGAR DATOS EXISTENTES (BATCH)
        // Nota: Asegúrate que tus Repositorios tengan los métodos findAllBy...In

        Map<String, AlumnoMatriculaEntity> matriculaMap = matriculaRepository.findAllByMatriculaIn(matriculas)
                .stream().collect(Collectors.toMap(AlumnoMatriculaEntity::getMatricula, m -> m));

        Map<String, AlumnoEntity> alumnoCorreoMap = alumnoRepository.findAllByCorreoIn(correos)
                .stream().collect(Collectors.toMap(AlumnoEntity::getCorreo, a -> a));

        // Aquí usamos el helper interno de esta clase
        Map<String, CarreraEntity> carreraMap = sincronizarCarrerasLocal(nombresCarreras);

        // 4. PROCESAR LOGICA EN MEMORIA
        // Usamos List en lugar de Map para guardar, es más simple si hay duplicados en el excel se sobreescriben
        List<AlumnoEntity> alumnosParaGuardar = new ArrayList<>();
        List<AlumnoMatriculaEntity> matriculasParaGuardar = new ArrayList<>();

        // Un map temporal para no duplicar objetos Alumno si aparecen varias veces en el Excel (raro pero posible)
        Map<String, AlumnoEntity> cacheAlumnosProcesados = new HashMap<>();

        for (AlumnoRow fila : filas) {
            AlumnoEntity alumno;

            // Lógica de resolución de Alumno
            AlumnoMatriculaEntity matriculaExistente = matriculaMap.get(fila.matricula());

            if (matriculaExistente != null) {
                alumno = matriculaExistente.getAlumno();
            } else if (fila.correo() != null && alumnoCorreoMap.containsKey(fila.correo())) {
                alumno = alumnoCorreoMap.get(fila.correo());
            } else {
                // Checar si ya lo creamos en este mismo loop (por si sale dos veces en el excel)
                String key = fila.correo() != null ? fila.correo() : fila.matricula();
                alumno = cacheAlumnosProcesados.getOrDefault(key, new AlumnoEntity());
                if (alumno.getId() == null) cacheAlumnosProcesados.put(key, alumno);
            }

            alumno.setNombreCompleto(fila.nombre());
            alumno.setCorreo(fila.correo());

            // Solo agregamos a la lista de guardar si no está ya (Hibernate hace merge, pero ahorramos memoria)
            if (!alumnosParaGuardar.contains(alumno)) {
                alumnosParaGuardar.add(alumno);
            }

            // Lógica de Matrícula
            AlumnoMatriculaEntity matricula = (matriculaExistente != null) ? matriculaExistente : new AlumnoMatriculaEntity();

            matricula.setAlumno(alumno); // Vinculación
            matricula.setMatricula(fila.matricula());
            matricula.setSemestre(fila.semestre());
            matricula.setActiva(true);

            if (fila.carrera() != null) {
                matricula.setCarrera(carreraMap.get(fila.carrera()));
            }

            matriculasParaGuardar.add(matricula);
        }

        // 5. GUARDADO MASIVO
        // Primero alumnos para generar IDs
        alumnoRepository.saveAll(alumnosParaGuardar);
        // Luego matrículas
        matriculaRepository.saveAll(matriculasParaGuardar);

        return filas.size();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    // Helper optimizado para buscar/crear carreras en lote
    private Map<String, CarreraEntity> sincronizarCarrerasLocal(Set<String> nombresRaw) {
        Set<String> nombres = nombresRaw.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());

        if (nombres.isEmpty()) return new HashMap<>();

        Map<String, CarreraEntity> mapa = carreraRepository.findAllByNombreIn(nombres)
                .stream().collect(Collectors.toMap(c -> c.getNombre().trim(), c -> c));

        List<CarreraEntity> nuevas = new ArrayList<>();
        for (String nombre : nombres) {
            if (!mapa.containsKey(nombre)) {
                CarreraEntity nueva = new CarreraEntity();
                nueva.setNombre(nombre);
                nuevas.add(nueva);
            }
        }

        if (!nuevas.isEmpty()) {
            List<CarreraEntity> guardadas = carreraRepository.saveAll(nuevas);
            for (CarreraEntity c : guardadas) {
                mapa.put(c.getNombre().trim(), c);
            }
        }
        return mapa;
    }

    // Helper individual (usado por importarDocentes)
    private CarreraEntity buscarOCrearCarreraIndividual(String nombre) {
        return carreraRepository.findByNombre(nombre.trim())
                .orElseGet(() -> {
                    CarreraEntity nueva = new CarreraEntity();
                    nueva.setNombre(nombre.trim());
                    return carreraRepository.save(nueva);
                });
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Integer.parseInt(cell.getStringCellValue());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // Record interno para el DTO temporal
    private record AlumnoRow(String nombre, String correo, String matricula, String carrera, Integer semestre) {}
}