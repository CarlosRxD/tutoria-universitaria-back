package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.CarreraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CarreraRepository extends JpaRepository<CarreraEntity, Integer> {

    // ─── Reconciliación con SICE ──────────────────────────────────────────────
    // codigoInterno = Id_Car del SICE (llave de reconciliación)
    Optional<CarreraEntity>     findByCodigoInterno(String codigoInterno);
    List<CarreraEntity>         findAllByCodigoInternoIn(Set<String> codigos);

    // ─── Búsqueda por nombre (fallback para datos migrados) ──────────────────
    Optional<CarreraEntity>     findByNombre(String nombre);
    List<CarreraEntity>         findAllByNombreIn(Set<String> nombres);
}