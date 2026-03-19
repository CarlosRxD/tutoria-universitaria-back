-- =============================================================================
-- V2: DATOS DE PRUEBA (CARGA INICIAL)
-- =============================================================================

-- 1. CARRERAS
INSERT INTO carreras (id, nombre, codigo_interno) VALUES
                                                      (1, 'Ingeniería en Computación', 'IC'),
                                                      (2, 'Ingeniería en Sistemas', 'IS'),
                                                      (3, 'Medicina Veterinaria y Zootecnia', 'MVZ'),
                                                      (4, 'Ingeniería Electrónica', 'IE'),
                                                      (5, 'Licenciatura en Administración', 'LA');

-- 2. AFINIDADES ENTRE CARRERAS
INSERT INTO afinidad_carreras (carrera_origen_id, carrera_destino_id, nivel_prioridad) VALUES
                                                                                           (1, 2, 1),  -- Computación -> Sistemas
                                                                                           (1, 4, 2),  -- Computación -> Electrónica
                                                                                           (2, 1, 1),  -- Sistemas -> Computación
                                                                                           (2, 4, 2),  -- Sistemas -> Electrónica
                                                                                           (4, 1, 1),  -- Electrónica -> Computación
                                                                                           (4, 2, 2);  -- Electrónica -> Sistemas

-- 3. DOCENTES
INSERT INTO docentes (id, nombre_completo, correo, estado, carrera_principal_id, max_tutorados) VALUES
                                                                                                    (1, 'Dr. Juan Carlos Martínez', 'juan.martinez@unpa.edu.mx', 'ACTIVO', 1, 13),
                                                                                                    (2, 'Dra. María Elena Rodríguez', 'maria.rodriguez@unpa.edu.mx', 'ACTIVO', 1, 13),
                                                                                                    (3, 'Mtro. Pedro Sánchez López', 'pedro.sanchez@unpa.edu.mx', 'ACTIVO', 1, 13),
                                                                                                    (4, 'Ing. Ana Laura Gómez', 'ana.gomez@unpa.edu.mx', 'ACTIVO', 2, 13),
                                                                                                    (5, 'Dr. Roberto García Ruiz', 'roberto.garcia@unpa.edu.mx', 'ACTIVO', 2, 13),
                                                                                                    (6, 'MVZ. Carmen Teresa Flores', 'carmen.flores@unpa.edu.mx', 'ACTIVO', 3, 13),
                                                                                                    (7, 'Dr. Luis Alberto Morales', 'luis.morales@unpa.edu.mx', 'SABATICO', 3, 13),
                                                                                                    (8, 'Ing. Francisco Javier Ortiz', 'francisco.ortiz@unpa.edu.mx', 'ACTIVO', 4, 13),
                                                                                                    (9, 'Dra. Gabriela Ramírez', 'gabriela.ramirez@unpa.edu.mx', 'ACTIVO', 4, 13),
                                                                                                    (10, 'Lic. Miguel Ángel Torres', 'miguel.torres@unpa.edu.mx', 'ACTIVO', 5, 13);

-- 4. ALUMNOS
INSERT INTO alumnos (id, nombre_completo, correo) VALUES
                                                      (1, 'Carlos Eduardo López Pérez', 'carlos.lopez@alumno.unpa.mx'),
                                                      (2, 'Diana Paola Hernández', 'diana.hernandez@alumno.unpa.mx'),
                                                      (3, 'Eduardo José Ramírez', 'eduardo.ramirez@alumno.unpa.mx'),
                                                      (4, 'Fernanda Guadalupe Castro', 'fernanda.castro@alumno.unpa.mx'),
                                                      (5, 'Gabriel Antonio Moreno', 'gabriel.moreno@alumno.unpa.mx'),
                                                      (6, 'Héctor Manuel Ortega', 'hector.ortega@alumno.unpa.mx'),
                                                      (7, 'Isabel Cristina Ruiz', 'isabel.ruiz@alumno.unpa.mx'),
                                                      (8, 'Jorge Luis Mendoza', 'jorge.mendoza@alumno.unpa.mx'),
                                                      (9, 'Karla Vanessa Jiménez', 'karla.jimenez@alumno.unpa.mx'),
                                                      (10, 'Leonardo Daniel Cruz', 'leonardo.cruz@alumno.unpa.mx'),
                                                      (11, 'Mariana Sofía Vargas', 'mariana.vargas@alumno.unpa.mx'),
                                                      (12, 'Nicolás Alejandro Reyes', 'nicolas.reyes@alumno.unpa.mx'),
                                                      (13, 'Olivia Patricia Domínguez', 'olivia.dominguez@alumno.unpa.mx'),
                                                      (14, 'Pablo Arturo Aguilar', 'pablo.aguilar@alumno.unpa.mx'),
                                                      (15, 'Quetzalli Montserrat Silva', 'quetzalli.silva@alumno.unpa.mx'),
                                                      (16, 'Ricardo Enrique Navarro', 'ricardo.navarro@alumno.unpa.mx'),
                                                      (17, 'Sandra Liliana Campos', 'sandra.campos@alumno.unpa.mx'),
                                                      (18, 'Tomás Sebastián Vega', 'tomas.vega@alumno.unpa.mx'),
                                                      (19, 'Úrsula Beatriz Molina', 'ursula.molina@alumno.unpa.mx'),
                                                      (20, 'Víctor Hugo Paredes', 'victor.paredes@alumno.unpa.mx'),
                                                      (21, 'Wendy Alejandra Ríos', 'wendy.rios@alumno.unpa.mx'),
                                                      (22, 'Xavier Fernando Cortés', 'xavier.cortes@alumno.unpa.mx'),
                                                      (23, 'Yaretzi Monserrat Luna', 'yaretzi.luna@alumno.unpa.mx'),
                                                      (24, 'Zacarías Ismael Rojas', 'zacarias.rojas@alumno.unpa.mx'),
                                                      (25, 'Adriana Michelle Guzmán', 'adriana.guzman@alumno.unpa.mx'),
                                                      (26, 'Benjamín Isaac Salazar', 'benjamin.salazar@alumno.unpa.mx'),
                                                      (27, 'Claudia Fernanda Medina', 'claudia.medina@alumno.unpa.mx'),
                                                      (28, 'Damián Alberto Ramos', 'damian.ramos@alumno.unpa.mx'),
                                                      (29, 'Elena Victoria Soto', 'elena.soto@alumno.unpa.mx'),
                                                      (30, 'Felipe Augusto Lara', 'felipe.lara@alumno.unpa.mx'),
                                                      (31, 'Giselle Paola Peña', 'giselle.pena@alumno.unpa.mx'),
                                                      (32, 'Héctor Rodrigo Cabrera', 'hector.cabrera@alumno.unpa.mx'),
                                                      (33, 'Irene Daniela Fuentes', 'irene.fuentes@alumno.unpa.mx');

-- 5. MATRÍCULAS DE ALUMNOS
INSERT INTO alumno_matriculas (alumno_id, matricula, carrera_id, semestre, activa) VALUES
                                                                                       (1, '21010101', 1, 3, true),
                                                                                       (2, '21010102', 1, 3, true),
                                                                                       (3, '21010103', 1, 5, true),
                                                                                       (4, '21010104', 1, 5, true),
                                                                                       (5, '21010105', 1, 7, true),
                                                                                       (6, '22010106', 1, 1, true),
                                                                                       (7, '22010107', 1, 1, true),
                                                                                       (8, '22010108', 1, 1, true),
                                                                                       (9, '22010109', 1, 3, true),
                                                                                       (10, '22010110', 1, 3, true),
                                                                                       (11, '23010111', 1, 1, true),
                                                                                       (12, '23010112', 1, 1, true),
                                                                                       (13, '23010113', 1, 1, true),
                                                                                       (14, '23010114', 1, 3, true),
                                                                                       (15, '23010115', 1, 3, true),
                                                                                       (16, '21020201', 2, 5, true),
                                                                                       (17, '21020202', 2, 5, true),
                                                                                       (18, '22020203', 2, 3, true),
                                                                                       (19, '22020204', 2, 3, true),
                                                                                       (20, '22020205', 2, 3, true),
                                                                                       (21, '23020206', 2, 1, true),
                                                                                       (22, '23020207', 2, 1, true),
                                                                                       (23, '23020208', 2, 1, true),
                                                                                       (24, '23020209', 2, 1, true),
                                                                                       (25, '23020210', 2, 1, true),
                                                                                       (26, '21030301', 3, 7, true),
                                                                                       (27, '21030302', 3, 7, true),
                                                                                       (28, '22030303', 3, 5, true),
                                                                                       (29, '22030304', 3, 5, true),
                                                                                       (30, '22030305', 3, 3, true),
                                                                                       (31, '23030306', 3, 1, true),
                                                                                       (32, '23030307', 3, 1, true),
                                                                                       (33, '23030308', 3, 1, true);

-- 6. ASIGNACIONES DEL PERIODO ANTERIOR
INSERT INTO asignaciones (periodo, alumno_id, docente_id, tipo, estado) VALUES
                                                                            ('24-25B', 1, 1, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 2, 1, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 3, 1, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 9, 1, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 10, 1, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 4, 2, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 5, 2, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 14, 2, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 15, 2, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 7, 3, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 8, 3, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 16, 4, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 17, 4, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 18, 4, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 19, 5, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 20, 5, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 26, 6, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 27, 6, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 28, 6, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 29, 7, 'ALEATORIO_CARRERA', 'CONFIRMADO'),
                                                                            ('24-25B', 30, 7, 'ALEATORIO_CARRERA', 'CONFIRMADO');

-- 7. SOLICITUDES PARA EL NUEVO PERIODO
INSERT INTO solicitudes_tutor (alumno_id, docente_id, periodo, fecha_solicitud) VALUES
                                                                                    (1, 3, '25-26A', '2025-01-15 10:30:00'),
                                                                                    (2, 1, '25-26A', '2025-01-15 11:00:00'),
                                                                                    (3, 2, '25-26A', '2025-01-15 14:20:00'),
                                                                                    (6, 1, '25-26A', '2025-01-16 09:00:00'),
                                                                                    (11, 2, '25-26A', '2025-01-16 09:15:00'),
                                                                                    (12, 3, '25-26A', '2025-01-16 10:00:00'),
                                                                                    (13, 1, '25-26A', '2025-01-16 10:30:00'),
                                                                                    (21, 4, '25-26A', '2025-01-17 08:00:00'),
                                                                                    (22, 5, '25-26A', '2025-01-17 08:30:00'),
                                                                                    (23, 4, '25-26A', '2025-01-17 09:00:00'),
                                                                                    (24, 5, '25-26A', '2025-01-17 09:30:00'),
                                                                                    (25, 4, '25-26A', '2025-01-17 10:00:00');

-- =============================================================================
-- 8. SINCRONIZACIÓN DE SECUENCIAS (IMPORTANTE PARA POSTGRES)
-- Esto evita el error "Duplicate Key" al crear nuevos registros desde Java
-- =============================================================================
SELECT setval('carreras_id_seq', (SELECT MAX(id) FROM carreras));
SELECT setval('afinidad_carreras_id_seq', (SELECT MAX(id) FROM afinidad_carreras));
SELECT setval('docentes_id_seq', (SELECT MAX(id) FROM docentes));
SELECT setval('alumnos_id_seq', (SELECT MAX(id) FROM alumnos));
SELECT setval('alumno_matriculas_id_seq', (SELECT MAX(id) FROM alumno_matriculas));
SELECT setval('asignaciones_id_seq', (SELECT MAX(id) FROM asignaciones));
SELECT setval('solicitudes_tutor_id_seq', (SELECT MAX(id) FROM solicitudes_tutor));