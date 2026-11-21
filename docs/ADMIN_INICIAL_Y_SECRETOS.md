# Administrador inicial por Liquibase 012 y configuración local

## Qué cambia
- `012-initial-admin.yaml` crea el administrador con hash BCrypt **ya generado** y asigna módulos.
- `AdminBootstrap.java` se elimina para evitar una segunda vía de creación y contraseñas en texto claro.
- La migración se ejecuta únicamente con el contexto `bootstrap-admin`. Por defecto está desactivada (`!bootstrap-admin`) para no afectar pruebas ni bases ya inicializadas.
- Si ya existe un ADMINISTRADOR activo, Liquibase marca la migración como ejecutada sin crear otro.

## Primera instalación (BD nueva)
1. Copiar `.env.example` a `.env`; elegir contraseñas propias. No subir `.env`.
2. Crear hash BCrypt localmente. Con Docker disponible, ejecutar `docker run --rm -it httpd:2.4-alpine htpasswd -nBC 12 admin`; introducir contraseña en el prompt, copiar **solo** la parte posterior a `admin:` (hash `$2y$...`). Spring Security acepta BCrypt `$2y$`.
3. Guardar el hash completo en `APP_ADMIN_PASSWORD_HASH` del `.env`. **Si se carga mediante PowerShell, usar comillas simples para conservar `$`**. El hash NO es la contraseña en texto claro.
4. Generar JWT secreto de 32 bytes con PowerShell:
   ```powershell
   $bytes = New-Object byte[] 32
   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $rng.GetBytes($bytes)
   $rng.Dispose()
   $env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
   ```
5. Cargar `.env` en el proceso de PowerShell (los valores se leen literalmente, incluidos los `$` del BCrypt):
   ```powershell
   Get-Content .env | Where-Object { $_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$' } | ForEach-Object {
       $name, $value = $_ -split '=', 2
       [Environment]::SetEnvironmentVariable($name, $value, 'Process')
   }
   ```
6. Confirmar que `APP_ADMIN_PASSWORD_HASH` comienza por `$2` y no es `REEMPLAZAR_CON_HASH_BCRYPT`. Establecer `APP_LIQUIBASE_CONTEXTS=bootstrap-admin` en primera instalación.
7. Ejecutar `docker compose up -d` y `./gradlew.bat bootRun`.
8. Verificar `SELECT u.email, u.activo FROM usuario u JOIN rol r ON r.id=u.rol_id WHERE r.nombre='ADMINISTRADOR';` y probar login.

**Importante:** Para el primer arranque no usar valores de ejemplo sin reemplazar. El contexto `bootstrap-admin` solo debe activarse con un hash BCrypt real. Si se deja desactivado en una BD nueva, no habrá administrador inicial. Después del primer arranque puede desactivarse para nuevos entornos, pero no afecta a migraciones ya ejecutadas.

## BD existente
La migración no cambia contraseñas ni usuarios existentes. Si ya existe un administrador activo, la precondición la marca ejecutada sin insertar otro. No borrar volúmenes ni ejecutar `docker compose down -v` sobre datos de trabajo.

## Pruebas
`./gradlew.bat clean test`. Validar migración 012 y login en una **BD nueva de prueba**, no sobre la BD de trabajo. Esta entrega no incluye evidencia de ejecución local de Gradle en Windows.

## Precauciones
- No versionar `.env`, JWT secret, contraseñas ni archivos adjuntos.
- El hash BCrypt de un usuario inicial no debe compartirse como credencial universal de producción.
- La URL JDBC y el puerto PostgreSQL deben corresponder a la instalación real.
