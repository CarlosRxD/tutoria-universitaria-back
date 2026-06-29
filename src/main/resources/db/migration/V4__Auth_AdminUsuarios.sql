-- ============================================================
--  V4: Tabla de usuarios administradores del sistema web CIT
--  Ejecutar manualmente (Flyway desactivado en este proyecto):
--    mysql -u admin_tutorias -p sga_tutorias < V4__Auth_AdminUsuarios.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS admin_usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    correo     VARCHAR(150) NOT NULL,
    password   VARCHAR(255) NOT NULL,   -- BCrypt hash
    nombre     VARCHAR(150) NOT NULL,
    rol        ENUM('ADMIN','COORDINADOR') NOT NULL DEFAULT 'ADMIN',
    activo     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT current_timestamp(),
    updated_at DATETIME     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  Usuario inicial: Dra. Carolina
--  Contraseña temporal: Admin@CIT2026
--  ⚠ Debe cambiarse después del primer inicio de sesión.
-- ============================================================
INSERT INTO admin_usuarios (correo, password, nombre, rol, activo)
VALUES (
    'carolinamaldonadomendez@gmail.com',
    '$2b$10$eNwMpUkocEHZegf3Zuu0eel665VTRQm5R3BoWCjiFFKZhibY3ht6S',
    'DRA. CAROLINA GABRIELA MALDONADO MENDEZ',
    'ADMIN',
    1
)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    rol    = VALUES(rol),
    activo = VALUES(activo);
