-- 1. Tipos ENUM (Se quedan igual)
DROP TYPE IF EXISTS estado_docente CASCADE;
DROP TYPE IF EXISTS estado_asignacion CASCADE;
DROP TYPE IF EXISTS tipo_asignacion CASCADE;

CREATE TYPE estado_docente AS ENUM ('ACTIVO', 'SABATICO', 'INHABIL', 'BAJA');
CREATE TYPE estado_asignacion AS ENUM ('PENDIENTE', 'ASIGNADO', 'CONFIRMADO');
CREATE TYPE tipo_asignacion AS ENUM ('ELECCION_ALUMNO', 'CONTINUIDAD', 'ALEATORIO_CARRERA', 'ALEATORIO_AFIN', 'ALEATORIO_GLOBAL');

-- 2. Tabla Carreras
-- (Se queda SERIAL porque en Java CarreraEntity usa Integer, no Long)
CREATE TABLE carreras (
                          id SERIAL PRIMARY KEY,
                          nombre VARCHAR(100) NOT NULL UNIQUE,
                          codigo_interno VARCHAR(20) UNIQUE
);

-- 3. Tabla Afinidad (CAMBIO: BIGSERIAL)
CREATE TABLE afinidad_carreras (
                                   id BIGSERIAL PRIMARY KEY,
                                   carrera_origen_id INT NOT NULL REFERENCES carreras(id),
                                   carrera_destino_id INT NOT NULL REFERENCES carreras(id),
                                   nivel_prioridad INT DEFAULT 1,
                                   UNIQUE (carrera_origen_id, carrera_destino_id)
);

-- 4. Tabla Docentes (CAMBIO: BIGSERIAL)
CREATE TABLE docentes (
                          id BIGSERIAL PRIMARY KEY,
                          nombre_completo VARCHAR(200) NOT NULL,
                          correo VARCHAR(150) UNIQUE NOT NULL,
                          estado estado_docente NOT NULL DEFAULT 'ACTIVO',
                          carrera_principal_id INT REFERENCES carreras(id),
                          max_tutorados INT NOT NULL DEFAULT 13 CHECK (max_tutorados > 0)
);

-- 5. Tabla Alumnos (CAMBIO: BIGSERIAL)
CREATE TABLE alumnos (
                         id BIGSERIAL PRIMARY KEY,
                         nombre_completo VARCHAR(200) NOT NULL,
                         correo VARCHAR(150)
);

-- 6. Tabla Matriculas (CAMBIO: BIGSERIAL y alumno_id ahora es BIGINT)
CREATE TABLE alumno_matriculas (
                                   id BIGSERIAL PRIMARY KEY,
                                   alumno_id BIGINT NOT NULL REFERENCES alumnos(id),
                                   matricula VARCHAR(20) UNIQUE NOT NULL,
                                   carrera_id INT REFERENCES carreras(id),
                                   semestre INT,
                                   activa BOOLEAN DEFAULT true
);

-- 7. Tabla Solicitudes (CAMBIO: BIGSERIAL y los IDs son BIGINT)
CREATE TABLE solicitudes_tutor (
                                   id BIGSERIAL PRIMARY KEY,
                                   alumno_id BIGINT NOT NULL REFERENCES alumnos(id),
                                   docente_id BIGINT NOT NULL REFERENCES docentes(id),
                                   periodo VARCHAR(20) NOT NULL,
                                   fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   UNIQUE (alumno_id, periodo)
);

-- 8. Tabla Asignaciones (CAMBIO: BIGSERIAL y los IDs son BIGINT)
CREATE TABLE asignaciones (
                              id BIGSERIAL PRIMARY KEY,
                              periodo VARCHAR(20) NOT NULL,
                              alumno_id BIGINT NOT NULL REFERENCES alumnos(id),
                              docente_id BIGINT NOT NULL REFERENCES docentes(id),
                              fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              tipo tipo_asignacion NOT NULL,
                              estado estado_asignacion DEFAULT 'ASIGNADO',
                              UNIQUE (periodo, alumno_id)
);

-- 9. Índices
CREATE INDEX idx_asignaciones_docente_periodo ON asignaciones(docente_id, periodo);
CREATE INDEX idx_matriculas_alumno ON alumno_matriculas(alumno_id);
CREATE INDEX idx_matriculas_matricula ON alumno_matriculas(matricula);