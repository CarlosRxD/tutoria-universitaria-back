package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.domain.model.EstadoDocente;
import mx.unpa.tutoria.infrastructure.entity.DocenteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DocenteRepository extends JpaRepository<DocenteEntity, Long> {

    // ─── Reconciliación con SICE (llave principal) ────────────────────────────
    Optional<DocenteEntity> findBySiceId(String siceId);
    List<DocenteEntity>     findAllBySiceIdIn(Set<String> siceIds);

    // ─── Fallback: buscar por correo o nombre (migración desde datos viejos) ──
    List<DocenteEntity> findAllByCorreoIn(Set<String> correos);
    List<DocenteEntity> findAllByNombreCompletoIn(Set<String> nombres);
    Optional<DocenteEntity> findByCorreo(String correo);
    Optional<DocenteEntity> findByNombreCompleto(String nombreCompleto);

    // ─── Consultas de negocio ─────────────────────────────────────────────────
    List<DocenteEntity> findByEstado(EstadoDocente estado);

    @Query("SELECT d FROM DocenteEntity d WHERE d.estado = 'ACTIVO'")
    List<DocenteEntity> findAllActivos();

    @Query("SELECT d FROM DocenteEntity d WHERE d.estado = 'ACTIVO' AND d.carreraPrincipal.id = :carreraId")
    List<DocenteEntity> findActivosByCarrera(@Param("carreraId") Integer carreraId);

    @Query("""
        SELECT d FROM DocenteEntity d
        WHERE d.estado IN ('ACTIVO', 'SABATICO', 'INHABIL')
        ORDER BY d.nombreCompleto
        """)
    List<DocenteEntity> findAllVigentes();

    // ─── Carga de docentes con conteo de tutorados por periodo ───────────────
    @Query("""
        SELECT d FROM DocenteEntity d
        LEFT JOIN FETCH d.carreraPrincipal
        WHERE d.estado NOT IN ('BAJA')
        ORDER BY d.nombreCompleto
        """)
    List<DocenteEntity> findAllConCarrera();
}