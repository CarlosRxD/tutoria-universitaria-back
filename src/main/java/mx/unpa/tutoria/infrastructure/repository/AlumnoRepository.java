package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.AlumnoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AlumnoRepository extends JpaRepository<AlumnoEntity, Long> {
    @Query("""
        SELECT DISTINCT a FROM AlumnoEntity a 
        LEFT JOIN FETCH a.matriculas m 
        LEFT JOIN FETCH m.carrera c 
        WHERE m.activa = true AND LOWER(a.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%'))
        """)
    List<AlumnoEntity> findActivosByNombreContaining(@Param("nombre") String nombre);
    List<AlumnoEntity> findAllByCorreoIn(Set<String> correos);
    Optional<AlumnoEntity> findByCorreo(String correo);
    
    List<AlumnoEntity> findByNombreCompletoContainingIgnoreCase(String nombre);

    @Query("""
        SELECT DISTINCT a FROM AlumnoEntity a 
        LEFT JOIN FETCH a.matriculas m 
        LEFT JOIN FETCH m.carrera c 
        WHERE m.activa = true
        """)
    List<AlumnoEntity> findAllConMatriculaActiva();
}
