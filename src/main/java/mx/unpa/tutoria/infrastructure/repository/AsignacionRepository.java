package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.AsignacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionRepository extends JpaRepository<AsignacionEntity, Long> {
    
    List<AsignacionEntity> findByPeriodo(String periodo);
    
    Optional<AsignacionEntity> findByAlumnoIdAndPeriodo(Long alumnoId, String periodo);
    
    List<AsignacionEntity> findByDocenteIdAndPeriodo(Long docenteId, String periodo);
    
    @Query("""
        SELECT COUNT(a) FROM AsignacionEntity a 
        WHERE a.docente.id = :docenteId 
        AND a.periodo = :periodo
        """)
    Long contarTutoradosPorDocenteYPeriodo(
        @Param("docenteId") Long docenteId, 
        @Param("periodo") String periodo
    );
    
    @Query("""
        SELECT a FROM AsignacionEntity a 
        WHERE a.periodo = :periodo 
        AND a.alumno.id IN (
            SELECT am.alumno.id FROM AlumnoMatriculaEntity am 
            WHERE am.carrera.id = :carreraId AND am.activa = true
        )
        """)
    List<AsignacionEntity> findByPeriodoYCarrera(
        @Param("periodo") String periodo, 
        @Param("carreraId") Integer carreraId
    );
    
    @Query("""
        SELECT a FROM AsignacionEntity a
        JOIN FETCH a.alumno
        JOIN FETCH a.docente
        WHERE a.docente.id = :docenteId AND a.periodo = :periodo
        """)
    List<AsignacionEntity> findTutoradosConDetalles(
        @Param("docenteId") Long docenteId,
        @Param("periodo") String periodo
    );

    @Query("""
    SELECT a FROM AsignacionEntity a 
    WHERE a.alumno.id = :alumnoId 
    ORDER BY a.fechaAsignacion DESC
    """)
    List<AsignacionEntity> findByAlumnoIdOrderByFechaAsignacionDesc(@Param("alumnoId") Long alumnoId);
}
