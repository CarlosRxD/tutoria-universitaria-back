-- =============================================================================
-- V3: CARGA REAL POR GRUPOS (ESTRATEGIA DE AFINIDAD)
-- =============================================================================

TRUNCATE TABLE solicitudes_tutor CASCADE;
TRUNCATE TABLE asignaciones CASCADE;
TRUNCATE TABLE alumno_matriculas CASCADE;
TRUNCATE TABLE alumnos CASCADE;
TRUNCATE TABLE afinidad_carreras CASCADE; -- Limpiamos afinidades viejas
TRUNCATE TABLE docentes CASCADE;
TRUNCATE TABLE carreras CASCADE;

ALTER SEQUENCE solicitudes_tutor_id_seq RESTART WITH 1;
ALTER SEQUENCE asignaciones_id_seq RESTART WITH 1;
ALTER SEQUENCE alumno_matriculas_id_seq RESTART WITH 1;
ALTER SEQUENCE alumnos_id_seq RESTART WITH 1;
ALTER SEQUENCE docentes_id_seq RESTART WITH 1;
ALTER SEQUENCE carreras_id_seq RESTART WITH 1;
ALTER SEQUENCE afinidad_carreras_id_seq RESTART WITH 1;

-- 1. CREAMOS CARRERAS REALES (ALUMNOS) Y CARRERAS "GENÉRICAS" (DOCENTES)
INSERT INTO carreras (id, nombre, codigo_interno) VALUES
-- Carreras Reales (Donde están los alumnos)
(1, 'Ingeniería en Computación', 'IC'),
(2, 'Ingeniería en Sistemas', 'IS'),
(3, 'Medicina Veterinaria y Zootecnia', 'MVZ'),
(4, 'Ingeniería Electrónica', 'IE'),
(5, 'Licenciatura en Administración', 'LA'),
(6, 'Ingeniería en Alimentos', 'IAL'),
(7, 'Ingeniería en Agronomía', 'IAG'),
-- Grupos de Profesores (Funcionan como carreras contenedoras)
(100, 'ACADEMIA DE AGROINGENIERÍA', 'GP_AGRO'),
(101, 'ACADEMIA DE ING. Y TECNOLOGÍA', 'GP_TEC'),
(102, 'ACADEMIA DE CIENCIAS AGROPECUARIAS', 'GP_CIEN'),
(103, 'CENTRO DE IDIOMAS', 'GP_IDI');

-- 2. CONFIGURAMOS LA AFINIDAD (LA MAGIA)
-- Esto permite que tu algoritmo asigne profes de "Grupos" a alumnos de "Carreras"
INSERT INTO afinidad_carreras (carrera_origen_id, carrera_destino_id, nivel_prioridad) VALUES
-- Si el alumno es de Computación (1), busca profes en Academia Tecnología (101)
(1, 101, 1),
-- Si el alumno es de Sistemas (2), busca profes en Academia Tecnología (101)
(2, 101, 1),
-- Si el alumno es de Electrónica (4), busca profes en Academia Tecnología (101)
(4, 101, 1),

-- Veterinaria (3) -> Academia Agropecuarias (102)
(3, 102, 1),
-- Agronomía (7) -> Academia Agropecuarias (102)
(7, 102, 1),

-- Alimentos (6) -> Academia Agroingeniería (100)
(6, 100, 1),

-- Administración (5) -> Puede tomar de Tecnología o Agroingeniería (Prioridad 2)
(5, 101, 2),
(5, 100, 2);


-- 3. INSERTAR DOCENTES (Asignados a sus ACADEMIAS, ID 100+)

-- GRUPO: INSTITUTO DE AGROINGENIERÍA (ID 100)
INSERT INTO docentes (nombre_completo, correo, estado, carrera_principal_id) VALUES
                                                                                 ('Dr. Axel Villavicencio', 'axelv@unpa.edu.mx', 'ACTIVO', 100),
                                                                                 ('Dr. Álvaro Cabrera', 'acabrera@unpa.edu.mx', 'ACTIVO', 100),
                                                                                 ('Dr. Francisco Gutiérrez', 'francisco.gutierrez@temporal.unpa', 'ACTIVO', 100),
                                                                                 ('Dr. Hiram Netzahualcóyotl García', 'hnetgarcia@unpa.edu.mx', 'ACTIVO', 100),
                                                                                 ('Dr. Jesús Santiaguillo', 'jsantiaguillo@unpa.edu.mx', 'ACTIVO', 100),
                                                                                 ('Dr. José Luis Juárez', 'joseluis.juarez@temporal.unpa', 'ACTIVO', 100),
                                                                                 ('Dr. Mauro Sánchez', 'mauro.sanchez@temporal.unpa', 'ACTIVO', 100),
                                                                                 ('M.C. Esteban Chávez', 'echavez@unpa.edu.mx', 'ACTIVO', 100),
                                                                                 ('D.C.O. Héctor Hugo Sánchez', 'hector.sanchez@temporal.unpa', 'ACTIVO', 100),
                                                                                 ('M.C. José Luis Nájera', 'jnajera@unpa.edu.mx', 'ACTIVO', 100),
                                                                                 ('M.C. Rafael F. González', 'rafael.gonzalez@temporal.unpa', 'ACTIVO', 100),
                                                                                 ('M.C. Olga Lidia Jimenez', 'olga.jimenez@temporal.unpa', 'ACTIVO', 100);

-- GRUPO: INGENIERÍA Y TECNOLOGÍA (ID 101)
INSERT INTO docentes (nombre_completo, correo, estado, carrera_principal_id) VALUES
                                                                                 ('Dr. Alberto Calixto', 'acalixto@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dra. Anahí Rojas', 'arojas@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dra. Aura Lucina Kantún', 'alkantun@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dra. Carolina Maldonado', 'cmaldonado@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dra. Eréndira Munguía', 'erendira.munguia@temporal.unpa', 'ACTIVO', 101),
                                                                                 ('Dr. Francisco Rendón', 'frendon@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dr. José Nobel Méndez', 'jmendez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dra. Laura Patricia Rivas', 'privas@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dra. Nancy Pérez', 'nperez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dr. Owen Abdalláh Borrás', 'aborras@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dr. Roberto Suárez', 'rsuarez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dr. Sergio Fabián Ruiz', 'sruiz@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('Dr. Víctor Manuel Méndez', 'vmendez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.B.A. Enrique Valdéz', 'enrique.valdez@temporal.unpa', 'ACTIVO', 101),
                                                                                 ('M.C. Ariel López', 'alopez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.C. Arturo Estrada', 'aestrada@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.C. José Benjamín Vergara', 'benjamin.vergara@temporal.unpa', 'ACTIVO', 101),
                                                                                 ('M.C. José Domingo Juárez', 'jdjuarez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.C. Luis Alberto Hernández', 'ahernandez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.C. Rey Fernando García', 'rfgarcia@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.C. Samira Belén Mayoral', 'smayoral@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.C. Sandro Geovani Vázquez', 'sgvazquez@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.F. José Arturo Martín', 'arturo.martin@temporal.unpa', 'ACTIVO', 101),
                                                                                 ('M.M. Iván Gpe. Mendoza', 'imendoza@unpa.edu.mx', 'ACTIVO', 101),
                                                                                 ('M.M.P. Carol Castro', 'ccastro@unpa.edu.mx', 'ACTIVO', 101);

-- GRUPO: CIENCIAS AGROPECUARIAS (ID 102)
INSERT INTO docentes (nombre_completo, correo, estado, carrera_principal_id) VALUES
                                                                                 ('Dr. Adolfo Amador', 'aamador@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dra. Amada I. Osorio T.', 'aosorio@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dra. Ana Rosa Ramírez', 'anaramirez@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Cecilio Ubaldo Aguilar', 'ubaldocuam@gmail.com', 'ACTIVO', 102),
                                                                                 ('Dr. Felipe Becerril', 'jelipano@yahoo.com.mx', 'ACTIVO', 102),
                                                                                 ('Dra. Gladis Morales', 'gmorales@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Hipólito Hernández', 'hhernandez@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. José Ángel Rueda', 'jrueda@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. José Antonio Yam', 'jyam@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. José Manuel Juarez', 'jmjuarez@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. José Orbelin Gutiérrez', 'jgutierrez@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Juan Pablo Alcántar', 'jpalcantar@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Marco Antonio Anzueto', 'manzueto@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dra. María Teresa Kido', 'tkido@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dra. Maribel Reyes', 'mreyes@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dra. Martha Elena Aguilera', 'meaguilera@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Miguel Á. Sánchez', 'msanchez@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Nicolás Valenzuela', 'nvalenzuela@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Rogelio Enrique Palacios', 'rpalacios@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Simeón Martínez de la Cruz', 'smartinez@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dra. Tania Zúñiga', 'tmarroquin@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('Dr. Wilber Hernández', 'wmontiel@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('M.C. Carlos Iván Medel', 'ivan_cito3@hotmail.com', 'ACTIVO', 102),
                                                                                 ('M.C. Edwin Aquino', 'eaquino@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('M.C. Hazaribagh García', 'hgarcia@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('M.C. Julián Cotera', 'julian.cotera@temporal.unpa', 'ACTIVO', 102),
                                                                                 ('M.C. Raúl Moreno', 'rmoreno@unpa.edu.mx', 'ACTIVO', 102),
                                                                                 ('M.E.R. José Antonio Marina', 'jose.marina@temporal.unpa', 'ACTIVO', 102),
                                                                                 ('L.B. Natalia Argüelles V', 'natalia.arguelles@temporal.unpa', 'ACTIVO', 102),
                                                                                 ('M.C. Noé Galindo D.', 'ngalindo@unpa.edu.mx', 'ACTIVO', 102);

-- GRUPO: CENTRO DE IDIOMAS (ID 103)
INSERT INTO docentes (nombre_completo, correo, estado, carrera_principal_id) VALUES
                                                                                 ('C. Cheryl Lynn Gad', 'cheryl@unpa.edu.mx', 'ACTIVO', 103),
                                                                                 ('Lic. James Patrick Killough', 'jpkillo@unpa.edu.mx', 'ACTIVO', 103);

-- 4. SINCRONIZAR SECUENCIAS
SELECT setval('carreras_id_seq', (SELECT MAX(id) FROM carreras));
SELECT setval('docentes_id_seq', (SELECT MAX(id) FROM docentes));
SELECT setval('afinidad_carreras_id_seq', (SELECT MAX(id) FROM afinidad_carreras));