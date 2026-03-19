package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.PeriodoEscolarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodoEscolarRepository extends JpaRepository<PeriodoEscolarEntity, PeriodoEscolarEntity.PeriodoId> {

    @Query(value = "SELECT CONCAT(Id_Cic_FK, '-', Id_Per) FROM periodosescolares WHERE Id_Per != 'V' ORDER BY Fechainicio_Per DESC LIMIT 2", nativeQuery = true)
    List<String> obtenerTodosLosPeriodos();

    @Query(value = "SELECT CONCAT(Id_Cic_FK, '-', Id_Per) FROM periodosescolares WHERE Id_Per != 'V' ORDER BY Fechainicio_Per DESC LIMIT 1", nativeQuery = true)
    String obtenerPeriodoMasReciente();
}