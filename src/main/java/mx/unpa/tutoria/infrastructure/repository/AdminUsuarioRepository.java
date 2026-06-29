package mx.unpa.tutoria.infrastructure.repository;

import mx.unpa.tutoria.infrastructure.entity.AdminUsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUsuarioRepository extends JpaRepository<AdminUsuarioEntity, Long> {

    Optional<AdminUsuarioEntity> findByCorreoAndActivoTrue(String correo);
}
