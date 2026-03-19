package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.AfinidadCarrerasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AfinidadCarrerasRepository extends JpaRepository<AfinidadCarrerasEntity, Long> {
    
    @Query("""
        SELECT ac FROM AfinidadCarrerasEntity ac 
        WHERE ac.carreraOrigen.id = :carreraId 
        ORDER BY ac.nivelPrioridad ASC
        """)
    List<AfinidadCarrerasEntity> findCarrerasAfinesPorPrioridad(@Param("carreraId") Integer carreraId);
}

