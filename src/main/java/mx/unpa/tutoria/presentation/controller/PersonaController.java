package mx.unpa.tutoria.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.unpa.tutoria.application.dto.AlumnoDTO;
import mx.unpa.tutoria.application.dto.DocenteDTO;
import mx.unpa.tutoria.domain.model.EstadoDocente;
import mx.unpa.tutoria.infrastructure.entity.AlumnoEntity;
import mx.unpa.tutoria.infrastructure.entity.AlumnoMatriculaEntity;
import mx.unpa.tutoria.infrastructure.entity.DocenteEntity;
import mx.unpa.tutoria.infrastructure.repository.AlumnoMatriculaRepository;
import mx.unpa.tutoria.infrastructure.repository.AlumnoRepository;
import mx.unpa.tutoria.infrastructure.repository.AsignacionRepository;
import mx.unpa.tutoria.infrastructure.repository.DocenteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:4200")
public class PersonaController {

    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoMatriculaRepository matriculaRepository;
    private final AsignacionRepository asignacionRepository;

    // ========== ENDPOINTS DE ALUMNOS ==========


    /**
     * Obtiene todos los alumnos
     */
    @GetMapping("/alumnos")
    public ResponseEntity<List<AlumnoDTO>> obtenerTodosLosAlumnos() {
        // 1. Hacemos UNA sola consulta que trae todo (Alumno + Matricula + Carrera)
        List<AlumnoEntity> alumnos = alumnoRepository.findAllConMatriculaActiva();

        // 2. Convertimos en memoria (CPU) sin ir a la base de datos
        List<AlumnoDTO> dtos = alumnos.stream()
                .map(this::convertirAlumnoADTOOptimum) // Usamos la versión optimizada
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
    private AlumnoDTO convertirAlumnoADTOOptimum(AlumnoEntity entity) {
        AlumnoDTO dto = new AlumnoDTO();
        dto.setId(entity.getId());
        dto.setNombreCompleto(entity.getNombreCompleto());
        dto.setCorreo(entity.getCorreo());

        // Como usamos JOIN FETCH, la lista de matrículas ya está cargada en memoria
        if (entity.getMatriculas() != null && !entity.getMatriculas().isEmpty()) {
            // Filtramos en memoria la activa (aunque el query ya trajo solo la activa, es doble seguridad)
            var matriculaActiva = entity.getMatriculas().stream()
                    .filter(m -> m.isActiva())
                    .findFirst();

            if (matriculaActiva.isPresent()) {
                var mat = matriculaActiva.get();
                dto.setMatriculaActiva(mat.getMatricula());
                dto.setSemestreActual(mat.getSemestre());
                if (mat.getCarrera() != null) {
                    dto.setCarreraActual(mat.getCarrera().getNombre());
                }
            }
        }
        return dto;
    }
    /**
     * Busca alumnos por nombre
     */
    @GetMapping("/alumnos/buscar")
    public ResponseEntity<List<AlumnoDTO>> buscarAlumnosPorNombre(
            @RequestParam String nombre) {

        // 1. Usamos la nueva consulta que ignora a los fantasmas
        List<AlumnoEntity> alumnos = alumnoRepository.findActivosByNombreContaining(nombre);

        // 2. Usamos el mapeador optimizado (el que no hace consultas extra)
        List<AlumnoDTO> dtos = alumnos.stream()
                .map(this::convertirAlumnoADTOOptimum)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca alumno por matrícula
     */
//    @GetMapping("/alumnos/matricula/{matricula}")
//    public ResponseEntity<AlumnoDTO> buscarAlumnoPorMatricula(
//            @PathVariable String matricula) {
//
//        Optional<AlumnoEntity> alumnoOpt = alumnoRepository.findByMatricula(matricula);
//
//        if (alumnoOpt.isPresent()) {
//            return ResponseEntity.ok(convertirAlumnoADTO(alumnoOpt.get()));
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }

    /**
     * Filtra alumnos por carrera
     */
//    @GetMapping("/alumnos/carrera/{carreraId}")
//    public ResponseEntity<List<AlumnoDTO>> filtrarAlumnosPorCarrera(
//            @PathVariable Integer carreraId) {
//
//        List<AlumnoEntity> alumnos = alumnoRepository.findByCarreraActiva(carreraId);
//
//        List<AlumnoDTO> dtos = alumnos.stream()
//            .map(this::convertirAlumnoADTO)
//            .collect(Collectors.toList());
//
//        return ResponseEntity.ok(dtos);
//    }

    // ========== ENDPOINTS DE DOCENTES ==========

    /**
     * Obtiene todos los docentes
     */
    @GetMapping("/docentes")
    public ResponseEntity<List<DocenteDTO>> obtenerTodosLosDocentes() {
        List<DocenteEntity> docentes = docenteRepository.findAll();
        List<DocenteDTO> dtos = docentes.stream()
            .map(d -> convertirDocenteADTO(d, null))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtiene solo docentes activos
     */
    @GetMapping("/docentes/activos")
    public ResponseEntity<List<DocenteDTO>> obtenerDocentesActivos() {
        List<DocenteEntity> docentes = docenteRepository.findByEstado(EstadoDocente.ACTIVO);
        List<DocenteDTO> dtos = docentes.stream()
            .map(d -> convertirDocenteADTO(d, null))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtiene docentes por carrera
     */
    @GetMapping("/docentes/carrera/{carreraId}")
    public ResponseEntity<List<DocenteDTO>> obtenerDocentesPorCarrera(
            @PathVariable Integer carreraId) {
        
        List<DocenteEntity> docentes = docenteRepository
            .findActivosByCarrera(carreraId);
        
        List<DocenteDTO> dtos = docentes.stream()
            .map(d -> convertirDocenteADTO(d, null))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtiene docentes con información de tutorados actuales para un periodo
     */
    @GetMapping("/docentes/periodo/{periodo}")
    public ResponseEntity<List<DocenteDTO>> obtenerDocentesConTutorados(
            @PathVariable String periodo) {

        List<DocenteEntity> docentes = docenteRepository.findAll();
        
        List<DocenteDTO> dtos = docentes.stream()
            .map(d -> convertirDocenteADTO(d, periodo))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtiene un docente por ID
     */
    @GetMapping("/docentes/{id}")
    public ResponseEntity<DocenteDTO> obtenerDocentePorId(@PathVariable Long id) {
        Optional<DocenteEntity> docenteOpt = docenteRepository.findById(id);
        
        if (docenteOpt.isPresent()) {
            return ResponseEntity.ok(convertirDocenteADTO(docenteOpt.get(), null));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== MÉTODOS DE CONVERSIÓN ==========

    private AlumnoDTO convertirAlumnoADTO(AlumnoEntity entity) {
        AlumnoDTO dto = new AlumnoDTO();
        dto.setId(entity.getId());
        dto.setNombreCompleto(entity.getNombreCompleto());
        dto.setCorreo(entity.getCorreo());
        
        // Obtener matrícula activa
        Optional<AlumnoMatriculaEntity> matriculaOpt = 
            matriculaRepository.findMatriculaActivaByAlumno(entity.getId());
        
        if (matriculaOpt.isPresent()) {
            AlumnoMatriculaEntity matricula = matriculaOpt.get();
            dto.setMatriculaActiva(matricula.getMatricula());
            dto.setSemestreActual(matricula.getSemestre());
            
            if (matricula.getCarrera() != null) {
                dto.setCarreraActual(matricula.getCarrera().getNombre());
            }
        }
        
        return dto;
    }

    private DocenteDTO convertirDocenteADTO(DocenteEntity entity, String periodo) {
        DocenteDTO dto = new DocenteDTO();
        dto.setId(entity.getId());
        dto.setNombreCompleto(entity.getNombreCompleto());
        dto.setCorreo(entity.getCorreo());
        dto.setEstado(entity.getEstado());
        dto.setMaxTutorados(entity.getMaxTutorados());
        
        if (entity.getCarreraPrincipal() != null) {
            dto.setCarreraPrincipal(entity.getCarreraPrincipal().getNombre());
        }
        
        // Si se proporciona periodo, obtener conteo de tutorados actuales
        if (periodo != null) {
            Long conteo = asignacionRepository
                .contarTutoradosPorDocenteYPeriodo(entity.getId(), periodo);
            dto.setTutoradosActuales(conteo.intValue());
        }
        
        return dto;
    }

    /**
     * Actualiza datos de un docente desde la interfaz de administración.
     *
     * REGLA CLAVE: Si la administradora edita correo o estado manualmente,
     * se activa tieneOverride=true para que la sincronización con el SICE
     * NO sobreescriba esos campos en el futuro.
     */
    @PutMapping("/docentes/{id}")
    public ResponseEntity<DocenteDTO> actualizarDocente(
            @PathVariable Long id,
            @RequestBody DocenteDTO datosActualizados) {

        Optional<DocenteEntity> docenteOpt = docenteRepository.findById(id);

        if (docenteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocenteEntity docente = docenteOpt.get();
        boolean huboEdicionManual = false;

        // Actualizar correo — si cambia, activar escudo
        if (datosActualizados.getCorreo() != null
                && !datosActualizados.getCorreo().equals(docente.getCorreo())) {
            docente.setCorreo(datosActualizados.getCorreo().trim().toLowerCase());
            huboEdicionManual = true;
        }

        // Actualizar estado — si cambia, activar escudo
        if (datosActualizados.getEstado() != null
                && !datosActualizados.getEstado().equals(docente.getEstado())) {
            docente.setEstado(datosActualizados.getEstado());
            huboEdicionManual = true;
        }

        // Actualizar maxTutorados — configurable, no activa override
        if (datosActualizados.getMaxTutorados() != null) {
            docente.setMaxTutorados(datosActualizados.getMaxTutorados());
        }

        // Si la admin editó correo o estado, protegemos contra sincronización
        if (huboEdicionManual) {
            docente.setTieneOverride(true);
        }

        DocenteEntity guardado = docenteRepository.save(docente);
        return ResponseEntity.ok(convertirDocenteADTO(guardado, null));
    }
}
