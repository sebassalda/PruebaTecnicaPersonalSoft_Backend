# BRP Offer Management — Backend

API REST para consultar un catálogo de Bienes Recibidos en Pago (BRP), radicar ofertas con oferentes y pagos, gestionar su aprobación, administrar usuarios y adjuntar documentos. Este repositorio contiene el backend; el frontend Angular se mantiene en un repositorio independiente: https://github.com/sebassalda/PruebaTecnicaPersonalSoft_Frontend.

## Tecnologías y requisitos

- Java **25** (toolchain definido en `build.gradle`), Spring Boot **4.2.0**, Gradle Wrapper, Spring Web MVC, Spring Security, Spring Data JPA y Bean Validation.
- PostgreSQL **17**, Liquibase, JWT y BCrypt.
- Docker Engine / Docker Desktop con Docker Compose para ejecutar backend y base de datos juntos.
- Puertos disponibles: **8080** para la API y **5433** para PostgreSQL desde el equipo anfitrión.

> El código está configurado para Java 25. Comprueba que el entorno de evaluación tenga disponible esa versión o utiliza la imagen construida con Docker.

## Configuración inicial (Docker Compose)

En la raíz del repositorio:

1. Copia `.env.example` a `.env` y reemplaza `DB_PASSWORD` y `APP_JWT_SECRET` por valores propios. No publiques el contenido de `.env`.
2. Copia `backend-secrets.env.example` a `backend-secrets.env` y reemplaza `APP_ADMIN_PASSWORD_HASH` por un hash BCrypt válido de la contraseña elegida para el administrador inicial. Este archivo **no debe versionarse**.
3. Comprueba `APP_ADMIN_EMAIL` en `.env` (valor predeterminado: `admin@brp.local`). La creación del administrador mediante Liquibase está pensada para una **base de datos nueva**; cambiar el hash después no cambia automáticamente la contraseña de un administrador ya creado.
4. Verifica la configuración y levanta los servicios:

```powershell
Copy-Item .env.example .env
Copy-Item backend-secrets.env.example backend-secrets.env
# Editar ambos archivos locales con valores reales antes de continuar.
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 backend
```

API: `http://localhost:8080`.

**Importante:** `docker-compose.yml` utiliza `.env` para interpolar las variables y `backend-secrets.env` como `env_file` del backend. Por ello, ambos son necesarios. No uses el texto de ejemplo como secreto o contraseña real. Para un secreto JWT utiliza un valor aleatorio suficientemente largo, compatible con la configuración de la aplicación; no reutilices credenciales de producción.

### Persistencia y puertos

| Recurso | Configuración |
|---|---|
| API en el anfitrión | `localhost:8080` |
| PostgreSQL en el anfitrión | `localhost:5433` |
| PostgreSQL desde el backend Docker | `postgres:5432` |
| Volumen de PostgreSQL | `brp_postgres_data` |
| Volumen de adjuntos | `brp_attachments` |
| Ruta de adjuntos en el contenedor | `/app/data/attachments` |

Para detener los servicios sin eliminar datos: `docker compose down`. **No ejecutar `docker compose down -v`** si se desea conservar la base de datos y los archivos adjuntos.

### Ejecución local alternativa

Con PostgreSQL disponible en `localhost:5433`, Java 25 y las variables de entorno necesarias (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_JWT_SECRET`), se puede iniciar con:

```powershell
.\gradlew.bat bootRun
```

En ejecución local, el contexto Liquibase `bootstrap-admin` está desactivado por defecto. Para inicializar un administrador en una base vacía, configura expresamente `APP_LIQUIBASE_CONTEXTS=bootstrap-admin` y `APP_ADMIN_PASSWORD_HASH` antes del primer arranque. Las variables definidas en `.env` no se exportan automáticamente al proceso `bootRun`.

## Modelo funcional y reglas de negocio

- **Catálogo BRP:** consulta paginada y detalle de bienes, con fecha de última ingestión. La carga inicial de 100 registros se realiza mediante migraciones Liquibase; el catálogo es de solo lectura desde la API.
- **Radicación:** el rol `RADICADOR`, con el módulo `OFFER_REGISTRATION`, registra una oferta vinculada a un BRP.
- **Oferentes y pagos:** cada oferta admite de **1 a 5 oferentes**, y cada oferente de **1 a 6 pagos**. La participación de los oferentes debe sumar **100 %**. Los pagos deben cuadrar tanto con el valor total de la oferta como con el porcentaje correspondiente a cada oferente.
- **Descuento:** se calcula frente al valor de referencia del BRP. Puede ser negativo cuando la oferta supera ese valor.
- **Estados:** `RADICADO` → `EN_PROCESO` → `ESCALADO` → `APROBADO` o `RECHAZADO`. El analista toma, escala o rechaza durante el análisis; el jefe aprueba, rechaza o devuelve al analista. La devolución conserva el analista asignado.
- **Seguridad:** permisos por rol y por módulo. Las operaciones sobre ofertas también aplican restricciones de acceso según el usuario y la oferta.
- **Concurrencia:** las actualizaciones del flujo usan control de concurrencia optimista; un conflicto se comunica con HTTP **409**.
- **Adjuntos:** PDF, JPEG o PNG, hasta **10 MB** por archivo; almacenamiento físico configurable y metadatos en la base de datos. Solo el radicador autorizado puede subir; el listado y la descarga requieren `OFFER_TRACKING`.

## Roles y módulos

| Rol | Responsabilidad principal |
|---|---|
| `RADICADOR` | Consulta de catálogo, radicación y seguimiento de sus ofertas, según módulos asignados |
| `ANALISTA` | Tomar, analizar, escalar o rechazar ofertas |
| `JEFE` | Aprobar, rechazar o devolver ofertas escaladas |
| `ADMINISTRADOR` | Administración de usuarios, módulos y permisos |

Los módulos relevantes incluyen `BRP_CATALOG`, `OFFER_REGISTRATION`, `OFFER_TRACKING`, `OFFER_MANAGEMENT`, `OFFER_APPROVAL` y `USER_MANAGEMENT`. El acceso efectivo requiere tanto el rol como el módulo apropiado.

## Endpoints principales

Todas las rutas, salvo el inicio de sesión, requieren un JWT en `Authorization: Bearer <token>` y los permisos correspondientes.

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/auth/login` | Autenticación |
| `GET` | `/api/brps?page=0&size=10` | Catálogo paginado |
| `GET` | `/api/brps/{id}` | Detalle del BRP |
| `GET` | `/api/brps/latest-ingestion` | Fecha de última ingestión |
| `POST` | `/api/offers` | Radicar oferta |
| `GET` | `/api/offers?page=0&size=10` | Seguimiento paginado |
| `GET` | `/api/offers/{id}` | Detalle de oferta |
| `POST` | `/api/offers/{id}/take` | Tomar oferta |
| `POST` | `/api/offers/{id}/escalate` | Escalar al jefe |
| `POST` | `/api/offers/{id}/analysis/reject` | Rechazo por analista |
| `POST` | `/api/offers/{id}/approve` | Aprobar por jefe |
| `POST` | `/api/offers/{id}/approval/reject` | Rechazo por jefe |
| `POST` | `/api/offers/{id}/return` | Devolver al analista |
| `POST` | `/api/offers/{offerId}/attachments` | Subir archivo multipart (`file`) |
| `GET` | `/api/offers/{offerId}/attachments` | Listar adjuntos |
| `GET` | `/api/offers/{offerId}/attachments/{attachmentId}` | Descargar adjunto |
| `GET` | `/api/users` | Listar usuarios (administrador) |
| `POST` | `/api/users` | Crear usuario |
| `PATCH` | `/api/users/{id}/status` | Activar o desactivar |
| `PUT` | `/api/users/{id}/modules` | Reemplazar módulos asignados |

### Ejemplos de solicitudes

Autenticación:

```http
POST /api/auth/login
Content-Type: application/json

{"email":"admin@brp.local","password":"<CONTRASEÑA_CONFIGURADA>"}
```

Radicación (ejemplo de estructura; el BRP debe existir y el usuario debe tener permisos):

```http
POST /api/offers
Authorization: Bearer <TOKEN_RADICADOR>
Content-Type: application/json

{
  "brpId": 1,
  "valorOferta": 300000000,
  "oferentes": [
    {
      "tipoIdentificacion": "CC",
      "numeroIdentificacion": "1020304050",
      "nombre": "Oferente de demostración",
      "porcentajeParticipacion": 100,
      "email": "demo@example.com",
      "telefono": "3001234567",
      "pagos": [
        {
          "tipoPago": "EFECTIVO",
          "valor": 300000000,
          "fechaPago": "2026-11-22",
          "entidadFinanciera": null
        }
      ]
    }
  ]
}
```

Adjunto: `POST /api/offers/{offerId}/attachments` con `multipart/form-data`, campo **`file`**. No establecer manualmente el encabezado `Content-Type` con boundary cuando se utiliza Postman o `FormData` del navegador.

## Usuarios de demostración

La instalación nueva puede crear el administrador con el correo configurado en `APP_ADMIN_EMAIL` (predeterminado `admin@brp.local`) y la contraseña correspondiente al hash BCrypt suministrado. **No hay una contraseña universal incorporada en este README**.

Para demostrar el flujo completo, crea o configura usuarios con los roles `RADICADOR`, `ANALISTA` y `JEFE` desde la administración o mediante el procedimiento de datos de demostración correspondiente. Anota en un canal seguro las contraseñas elegidas para la evaluación; no las publiques en Git.

## Pruebas automatizadas

```powershell
.\gradlew.bat clean test
```

Los resultados se generan en `build/test-results/test` y el reporte HTML en `build/reports/tests/test/index.html`. Este comando debe ejecutarse en el entorno final antes de entregar; no se asume que un reporte anterior garantice el resultado de una compilación posterior.

## Guion de demostración

1. Iniciar Docker Compose y verificar que el backend responde.
2. Iniciar sesión como administrador; verificar usuarios y asignación de módulos.
3. Iniciar sesión como radicador; consultar catálogo y última ingestión.
4. Crear una oferta válida con oferentes y pagos; comprobar estado `RADICADO`.
5. Subir un PDF a la oferta; listar y descargar el adjunto.
6. Iniciar sesión como analista; tomar la oferta (`EN_PROCESO`) y escalarla (`ESCALADO`).
7. Iniciar sesión como jefe; aprobarla (`APROBADO`).
8. Repetir con otra oferta para demostrar rechazo o devolución al analista.
9. Comprobar controles de acceso y el tratamiento de conflictos HTTP 409.

## Frontend

La interfaz Angular se encuentra en el repositorio independiente [PruebaTecnicaPersonalSoft_Frontend](https://github.com/sebassalda/PruebaTecnicaPersonalSoft_Frontend). En desarrollo utiliza el proxy `/api` hacia `http://localhost:8080`. Consultar su documentación para instalar dependencias y ejecutar `npm start` o el comando de Angular configurado en ese repositorio.

## Seguridad y consideraciones de entrega

- No publicar `.env`, `backend-secrets.env`, JWT, contraseñas ni credenciales reales.
- `backend-secrets.env` estuvo previamente versionado; quitarlo del índice no borra su historial. Rotar cualquier secreto real o reutilizado que haya estado expuesto y evaluar el tratamiento del historial antes de compartir el repositorio.
- El archivo `.env.example` contiene valores de referencia, no credenciales utilizables.
- No eliminar los volúmenes de Docker si se necesita conservar el catálogo, los usuarios o los adjuntos.

## Decisiones técnicas y respuestas del enunciado

Las respuestas a las ocho preguntas de la sección 9 se encuentran
en [TECHNICAL_DECISIONS.md](TECHNICAL_DECISIONS.md).