# Bloque 10 - Docker backend + PostgreSQL

Este parche agrega el backend al Compose existente y conserva el nombre `brp-postgres` y el volumen `brp_postgres_data`. No ejecuta borrados.

## Antes de levantar

- Mantener `.env` fuera de Git. `DB_PASSWORD`, `APP_JWT_SECRET` y `APP_ADMIN_PASSWORD_HASH` deben estar definidos.
- El hash BCrypt debe corresponder a la contraseña inicial que usará el evaluador. El hash se genera **una vez**; no hay que repetir JShell en cada arranque. No publicar el secreto JWT ni la contraseña.
- Revisar que `APP_ADMIN_EMAIL` corresponda al administrador deseado en una instalación nueva.
- Detener el `bootRun` local para liberar el puerto 8080.
- Si se usaron variables `$env:DB_URL` y `$env:APP_LIQUIBASE_CONTEXTS` temporales para la prueba, Docker Compose NO las usa para el backend; el contenedor utiliza la base `${DB_NAME}` y el contexto `bootstrap-admin` definidos en Compose.

## Comandos

```powershell
docker compose config --quiet
.\gradlew.bat clean test
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 backend
```

El backend queda en `http://localhost:8080`. En una BD vacía Liquibase crea el esquema y, mediante el contexto `bootstrap-admin`, el administrador. En la BD principal existente, la precondición de la migración 012 detecta el administrador activo y omite la inserción. No modifica su contraseña.

**Importante**: `docker compose config --quiet` valida sintaxis e interpolación, no confirma que la imagen compile. `docker compose up --build -d` necesita Docker disponible. Este parche no incluye frontend Angular; se añadirá cuando exista el proyecto frontend.

## Precauciones

No ejecutar `docker compose down -v`: elimina volúmenes. Para detener los servicios sin borrar datos: `docker compose down`.

En Docker, el backend usa `postgres:5432` (red interna), no `localhost:5433` (acceso desde Windows). Los adjuntos persisten en `brp_attachments`.

La imagen usa Java 25 porque el `build.gradle` de la versión de backend revisada solicita toolchain Java 25. Antes de entregar, contrastar con el requisito de Java de la prueba técnica original.
