package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.AlumnoMatriculaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AlumnoMatriculaRepository extends JpaRepository<AlumnoMatriculaEntity, Long> {
    List<AlumnoMatriculaEntity> findAllByMatriculaIn(Set<String> matriculas);
    Optional<AlumnoMatriculaEntity> findByMatricula(String matricula);
    
    List<AlumnoMatriculaEntity> findByAlumnoId(Long alumnoId);

    @Query("""
    SELECT am FROM AlumnoMatriculaEntity am 
    WHERE am.alumno.id = :alumnoId AND am.activa = true
    """)
    List<AlumnoMatriculaEntity> findMatriculasActivasByAlumno(@Param("alumnoId") Long alumnoId);

    // Y agregamos un método default (por defecto) en la misma interfaz para mantener el Optional
    default Optional<AlumnoMatriculaEntity> findMatriculaActivaByAlumno(Long alumnoId) {
        return findMatriculasActivasByAlumno(alumnoId).stream().findFirst();
    }
    
    List<AlumnoMatriculaEntity> findByCarreraIdAndActiva(Integer carreraId, boolean activa);

    @Query("SELECT am FROM AlumnoMatriculaEntity am WHERE am.activa = true")
    List<AlumnoMatriculaEntity> findAllActivas();
}
