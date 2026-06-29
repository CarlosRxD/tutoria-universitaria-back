# Guía de Desarrollo, Control de Versiones y Despliegue (CIT)

Este documento explica el flujo de trabajo estándar para realizar cambios en el proyecto CIT, construir imágenes de Docker y desplegar las actualizaciones en el servidor.

---

## 1. Control de Versiones y Flujo de Trabajo (Git)

Para mantener el código ordenado y evitar conflictos entre desarrolladores, se debe seguir un flujo de trabajo basado en ramas (Git Branching):

1. **Nunca trabajes directamente en la rama `main`.**
2. **Crear una rama para cada cambio o característica:**
   Antes de empezar a codificar, crea una rama descriptiva basada en `main`.
   ```bash
   # Asegúrate de estar en main y actualizado
   git checkout main
   git pull origin main

   # Crea y muévete a tu nueva rama
   git checkout -b feature/nombre-del-cambio
   ```
3. **Realizar modificaciones y probar localmente.**
4. **Hacer commits limpios:**
   ```bash
   git add .
   git commit -m "Descripción clara del cambio realizado"
   ```
5. **Subir la rama a GitHub:**
   ```bash
   git push origin feature/nombre-del-cambio
   ```
6. **Crear un Pull Request (PR)** en GitHub para integrar los cambios a `main`. Una vez aprobado y fusionado (merged), se procede al despliegue.

---

## 2. Conexión al Servidor mediante SSH (Servidor Puente / Jump Host)

El servidor del sistema se encuentra en una red interna y requiere conectarse a través de un servidor puente con IP pública.

### Configuración del archivo `config` de SSH
En tu máquina de desarrollo (Windows/Linux/Mac), edita o crea el archivo en `~/.ssh/config` y añade la siguiente configuración:

```text
# Servidor Puente (El que tiene IP pública)
Host puente
    HostName 201.144.254.11
    User ingcomputacion

# Servidor Final (El que está en la red interna)
Host server
    HostName 192.168.122.71
    User sgct
    ProxyJump puente
```

### Conexión rápida
Una vez configurado el archivo anterior, puedes conectarte directamente al servidor final ejecutando en tu terminal:
```bash
ssh server
```

### Credenciales de Acceso
* **Contraseña de los usuarios de SSH** (`ingcomputacion` y `sgct`): `icomp2025*`
* **Contraseña del usuario administrador (`root`)**: `icomp2025#`

---

## 3. Construcción y Subida de Imágenes Docker

El despliegue se maneja a través de contenedores Docker utilizando un sistema de etiquetado por versiones (tags).

> [!IMPORTANT]
> * **Ruta Local**: Reemplaza `[tu-ruta-local]` por la ruta donde clonaste la carpeta `CIT` en tu máquina (ej. `C:\Users\nombre-usuario\Documentos`).
> * **Usuario Docker**: Reemplaza `[tu-usuario-docker]` por el usuario de Docker Hub donde subirás la imagen (ej. `carlosrcr` para la cuenta de producción oficial). Asegúrate de iniciar sesión antes con `docker login`.

### Backend
1. Navega al proyecto backend: `[tu-ruta-local]/CIT/Backend/tutoria-universitaria`
2. Construye la imagen Docker especificando la nueva versión (ejemplo: incrementa a `v25` si la actual es `v24`):
   ```bash
   docker build -t [tu-usuario-docker]/tutoria-backend:v25 .
   ```
3. Sube la imagen construida al registro de Docker Hub:
   ```bash
   docker push [tu-usuario-docker]/tutoria-backend:v25
   ```

### Frontend
1. Navega al proyecto frontend: `[tu-ruta-local]/CIT/Frontend/tutoria-universitaria`
2. Construye la imagen Docker con su respectiva versión (ejemplo: incrementa a `v10` si la actual es `v9`):
   ```bash
   docker build -t [tu-usuario-docker]/tutoria-frontend:v10 .
   ```
3. Sube la imagen construida a Docker Hub:
   ```bash
   docker push [tu-usuario-docker]/tutoria-frontend:v10
   ```

---

## 4. Despliegue en el Servidor

Una vez subida la imagen a Docker Hub, debes ingresar al servidor para descargar los cambios y levantar los nuevos contenedores.

### Pasos para actualizar:

1. **Conéctate al servidor final:**
   ```bash
   ssh server
   ```
2. **Accede como usuario administrador (`root`):**
   ```bash
   su
   # Introduce la contraseña: icomp2025#
   ```
3. **Navega al directorio del administrador (`/root`):**
   ```bash
   cd ~
   ```
4. **Ejecutar el script de actualización correspondiente:**

   * **Para actualizar el Backend (ejemplo versión `v25`):**
     ```bash
     ./actualizar.sh v25
     ```
   * **Para actualizar el Frontend (ejemplo versión `v10`):**
     ```bash
     ./actualizar_front.sh v10
     ```

*Nota: Asegúrate de usar la misma etiqueta (versión) con la que construiste y subiste la imagen a Docker Hub.*

---
*Última actualización de versiones en producción (29 de Junio de 2026):*
* **Backend**: `v24`
* **Frontend**: `v9`
