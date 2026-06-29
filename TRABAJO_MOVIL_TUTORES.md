# Documentación de Integración Móvil - Módulo de Tutores

Este documento detalla el desarrollo y la lógica implementada en la aplicación móvil del sistema, cuyo código fuente se encuentra en un repositorio externo de calificaciones de la UNPA.

---

## 1. Repositorio del Proyecto Móvil

El desarrollo de este módulo se encuentra alojado en:
* **Repositorio**: [CalificacionesUNPA (RodolfoMarinero)](https://github.com/RodolfoMarinero/CalificacionesUNPA)
* **Rama de Trabajo**: `feature/api-movil`
* **Tecnología**: Aplicación Móvil

---

## 2. Descripción General del Módulo

El **Módulo de Tutores** dentro de la aplicación móvil está diseñado para que los alumnos puedan gestionar e informarse sobre sus tutorías académicas desde sus dispositivos. Su propósito principal es brindar transparencia en las asignaciones de tutores y permitir a los alumnos solicitar cambios de tutor de forma autónoma durante los periodos autorizados.

---

## 3. Funcionalidades Clave Desarrolladas

### A. Consulta de Tutor Actual
* El alumno puede visualizar de forma inmediata quién es su tutor asignado para el periodo escolar vigente.
* Se muestra la información de contacto básica del tutor: **Nombre completo** y **Correo electrónico institucional**.

### B. Historial de Tutorías
* Permite al alumno revisar el histórico de tutores que ha tenido asignados en ciclos o periodos pasados, facilitando el seguimiento de su trayectoria escolar.

### C. Solicitud/Cambio de Tutor (Ventana de Cambio del CIT)
Cuando el Comité de Tutorías (CIT) habilita la ventana temporal para solicitar cambios de tutor en el sistema, la aplicación móvil activa la interfaz correspondiente con las siguientes opciones:

1. **Selección de Docentes de la Misma Carrera**:
   * Muestra la lista de todos los docentes disponibles que pertenecen a la misma carrera del alumno.
   * El alumno puede seleccionar un nuevo docente de esta lista para solicitar el cambio, o decidir **no realizar ningún cambio** y permanecer con su tutor actual.
2. **Selección de Docentes de Otras Carreras (Interdisciplinario)**:
   * El alumno tiene la posibilidad de explorar y ver la lista de docentes adscritos a otras carreras de la universidad.
   * Esto permite al alumno elegir un tutor de un área de estudio distinta si considera que es más afín a sus necesidades académicas o profesionales.

---

## 4. Flujo de Control en la API y Base de Datos

El backend de tutores (`tutoria-universitaria`) provee las APIs correspondientes que consume este módulo móvil, validando:
1. Si la fecha actual se encuentra dentro del rango de la ventana de cambios configurada por el CIT.
2. La carga académica del tutor seleccionado para no superar el límite de alumnos asignados por docente.
3. El correcto almacenamiento de la solicitud en la base de datos de tutorías.
