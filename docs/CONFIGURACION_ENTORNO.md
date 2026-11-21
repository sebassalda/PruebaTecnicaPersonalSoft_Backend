# Configuración reproducible — BRP Offer Management

## Estado de la versión

El ZIP (3) todavía crea el administrador con `AdminBootstrap` (APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD). La migración de administrador con hash BCrypt pre-generado es **pendiente**; no se debe declarar completada.

## Variables

Copiar `.env.example` a `.env` y reemplazar contraseñas y secreto. `.env` está excluido de Git.

En PowerShell, para crear el secreto JWT compatible con Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Para cargar las variables del archivo `.env` en la sesión actual de PowerShell (no ejecutar archivos de origen desconocido):

```powershell
Get-Content .env | Where-Object { $_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
```

`docker compose up -d` inicia PostgreSQL. Luego `./gradlew.bat bootRun` inicia Spring Boot. En Windows PowerShell usar `./gradlew.bat` o `.\gradlew.bat`. No subir `.env` ni contraseñas reales al repositorio.

## Validación

Ejecutar `./gradlew.bat clean test`. Para validar instalación limpia, usar una base de datos o volumen de prueba **independiente**; no ejecutar `docker compose down -v` sobre el volumen que contiene los datos de las pruebas manuales.

## Pendientes de seguridad

- Implementar migración 012 de administrador inicial con hash BCrypt y sin credenciales reales en Git.
- Retirar o deshabilitar `AdminBootstrap` una vez validada la migración, evitando duplicidad.
- Probar protección de `USER_MANAGEMENT` del propio administrador y último administrador activo.
