package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.SolicitudTutorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudTutorRepository extends JpaRepository<SolicitudTutorEntity, Long> {
    
    Optional<SolicitudTutorEntity> findByAlumnoIdAndPeriodo(Long alumnoId, String periodo);
    
    List<SolicitudTutorEntity> findByPeriodo(String periodo);
    
    @Query("""
        SELECT COUNT(s) FROM SolicitudTutorEntity s 
        WHERE s.docente.id = :docenteId 
        AND s.periodo = :periodo
        """)
    Long contarSolicitudesPorDocente(
        @Param("docenteId") Long docenteId,
        @Param("periodo") String periodo
    );
    
    @Query("""
        SELECT s FROM SolicitudTutorEntity s 
        WHERE s.periodo = :periodo 
        ORDER BY s.fechaSolicitud ASC
        """)
    List<SolicitudTutorEntity> findByPeriodoOrdenadoPorFecha(@Param("periodo") String periodo);
}
