# Decisiones técnicas — BRP Offer Management

Este documento responde las ocho preguntas de la sección 9 del enunciado de la prueba técnica. Describe el enfoque utilizado en la demo y distingue lo implementado de las posibles mejoras para un sistema productivo.

## 1. ¿Dónde vive la regla de transiciones y cómo agregaría un estado nuevo sin tocar varios archivos?

Las operaciones HTTP se exponen mediante `OfferTransitionController`, mientras que las reglas del flujo se implementan en `OfferTransitionServiceImpl`. La decisión de permitir una transición corresponde al backend: la interfaz únicamente presenta las acciones que el usuario puede solicitar.

El flujo contempla `RADICADO → EN_PROCESO → ESCALADO → APROBADO / RECHAZADO`. El analista toma una oferta radicada y puede escalarla o rechazarla; el jefe puede aprobar, rechazar o devolver una oferta escalada a `EN_PROCESO`, conservando el analista asignado. Las transiciones incompatibles con el estado o las reglas del flujo se rechazan.

Para incorporar un estado nuevo, primero definiría sus transiciones permitidas, actores autorizados y efectos sobre fechas y asignaciones. **En la implementación actual sería necesario revisar los puntos del código que conocen los estados**; no afirmo que la incorporación sea automática o que se resuelva modificando un único archivo. Como evolución, centralizaría la matriz de transiciones y sus políticas en una definición de dominio —por ejemplo, una tabla de reglas o una máquina de estados— para reducir las modificaciones dispersas. Mantendría pruebas para las transiciones permitidas y prohibidas, además de actualizar el contrato y la interfaz cuando correspondiera.

## 2. ¿Cómo evité acciones simultáneas sobre la misma oferta y qué ve el usuario ante un conflicto?

La entidad `OfferEntity` utiliza `@Version` de JPA para aplicar **concurrencia optimista**. De esta forma, una operación que intenta guardar una versión desactualizada no puede sobrescribir silenciosamente el cambio realizado por otra transacción.

En `OfferTransitionServiceImpl`, `saveAndFlush` fuerza la comprobación de versión dentro de la transacción. `GlobalExceptionHandler` contempla `ObjectOptimisticLockingFailureException` y `OptimisticLockException` para traducir los conflictos de concurrencia a una respuesta HTTP **409 Conflict**. Las transiciones inválidas también deben informarse como conflictos del flujo.

En Angular, cuando una acción devuelve 409, se muestra un mensaje indicando que la oferta cambió de estado o que existe un conflicto y se solicita actualizar la página. Después de una transición exitosa, la vista vuelve a consultar el detalle para mostrar los datos actualizados. Este enfoque prioriza la consistencia sin mantener bloqueos de base de datos mientras el usuario decide qué acción realizar.

## 3. ¿Cómo manejé el JWT: duración, revocación y refresh tokens?

La autenticación utiliza usuarios propios, contraseñas protegidas con BCrypt y JWT procesados mediante Spring Security. `JwtConfig` configura `NimbusJwtEncoder` y `NimbusJwtDecoder`, mientras que `JwtService` se encarga de la emisión de tokens.

La duración del token es configurable con `APP_JWT_EXPIRATION_MINUTES` (60 minutos por defecto). En el frontend, `AuthService` verifica la expiración del token y elimina la sesión local cuando detecta que ha vencido; el interceptor también contempla respuestas de autenticación no autorizada.

**No se implementaron refresh tokens ni un mecanismo de revocación inmediata de JWT emitidos.** El cierre de sesión elimina las credenciales almacenadas en el navegador, pero no equivale a invalidar un token todavía vigente en el servidor. Para una aplicación productiva evaluaría renovación con tokens rotativos, revocación por sesión y políticas adicionales para cambios de permisos. No se incorporan en esta demo porque requieren diseño, endpoints y pruebas de seguridad adicionales y no forman parte de los requisitos obligatorios de la entrega.

## 4. ¿Qué decisiones tomé para usuarios nuevos y qué mejoraría?

Priorizé que los flujos principales fueran comprensibles sin capacitación previa. Después del inicio de sesión, Angular presenta una página de inicio con opciones filtradas por los módulos y el rol del usuario. Las pantallas emplean Angular Material y formularios reactivos, con validaciones y mensajes para orientar el registro de ofertas.

La pantalla de detalle reúne la información general, los oferentes, los pagos, el estado y las acciones disponibles según el perfil. Las acciones de gestión requieren confirmación y muestran el resultado o el error correspondiente. Los adjuntos pueden consultarse y descargarse desde el detalle; su carga se muestra únicamente a un radicador autorizado. Las restricciones importantes se verifican también en el backend, no solo mediante controles visuales.

Como mejora de bajo riesgo, revisaría la jerarquía visual, la consistencia de espacios y encabezados, la diferenciación de estados y la claridad de los mensajes. Estos ajustes se limitan al frontend y no alteran las reglas de negocio ni los endpoints existentes.

## 5. ¿Qué encontré ambiguo o incompleto, qué supuse y qué preguntaría al negocio?

El enunciado define con claridad las reglas centrales del catálogo, los oferentes, los pagos, las transiciones, la separación de funciones y la visibilidad por rol. Esos puntos **son requisitos explícitos, no supuestos**. Las decisiones adicionales surgieron al resolver detalles que el documento no especificaba completamente:

- **Política de adjuntos:** se exige almacenar los archivos en disco y guardar sus metadatos, pero no se fijan tipos, tamaño máximo ni política de eliminación. Para la demo se admitieron PDF, JPEG y PNG, con un máximo de 10 MB por archivo. No se incorporó eliminación de adjuntos.
- **Experiencia ante conflictos:** se exige devolver HTTP 409 ante conflictos, pero no se prescribe el mensaje ni la interacción exacta. Se optó por informar el conflicto y pedir una actualización de los datos antes de reintentar.
- **Administración y recuperación de acceso:** se detallan los permisos y la administración de usuarios, pero no un flujo de recuperación de contraseña, revocación de sesiones o renovación de tokens. No se asumió que esas capacidades fueran obligatorias.
- **Permisos parciales:** el documento define roles y módulos, pero no una matriz exhaustiva de todas las combinaciones de asignación posibles. Se mantuvo la validación de rol y módulo en el servidor y se construyó la navegación con los permisos disponibles para la sesión.

Antes de llevar el producto a producción, preguntaría al equipo de negocio por la política de conservación y eliminación documental, las combinaciones permitidas de módulos, los requisitos de recuperación de acceso y los mensajes esperados para cada escenario de error. Mejoraría el enunciado añadiendo una matriz de autorización detallada, criterios de aceptación para adjuntos y ejemplos de respuestas de error.

## 6. ¿Qué quedó pendiente y por qué?

La entrega priorizó el backend, las reglas de negocio, el control de acceso, el flujo de estados, las migraciones, la carga del catálogo y una interfaz Angular funcional, de acuerdo con el peso relativo indicado en la prueba. Se integró la carga, consulta y descarga de adjuntos en el detalle de oferta y se comprobó manualmente su funcionamiento con un usuario radicador.

No se incorporaron recuperación de contraseña, refresh tokens, revocación inmediata de sesiones ni eliminación de adjuntos. Tampoco se plantea como parte del alcance actual una plataforma de observabilidad avanzada o una batería completa de pruebas end-to-end. Son extensiones que requerirían especificación, implementación y validación adicionales; no se presentan como requisitos obligatorios incumplidos.

La mejora visual del frontend se considera opcional y acotada: se realizará únicamente si queda tiempo, sin cambiar el comportamiento del backend. Antes de entregar corresponde revisar la documentación, la configuración de arranque y los escenarios principales de la demo.

## 7. ¿Utilicé inteligencia artificial? ¿Cómo y para qué?

Sí. Utilicé **ChatGPT** como herramienta de asistencia técnica a lo largo del proyecto: análisis de requisitos, organización del trabajo, evaluación de alternativas, desarrollo, revisión de código, depuración, preparación de pruebas y documentación.

El proceso fue iterativo. A partir del enunciado y de las decisiones que iba tomando, formulé preguntas, expuse escenarios concretos, compartí implementaciones y resultados de ejecución, y planteé observaciones para corregir o mejorar las propuestas. La herramienta ayudó a identificar alternativas y posibles problemas; las respuestas no se trataron como resultados automáticamente válidos.

Las correcciones se contrastaron con los requisitos y con el comportamiento de la aplicación. Se realizaron compilaciones, pruebas automatizadas durante el desarrollo y verificaciones manuales de endpoints y flujos de la interfaz. Por ejemplo, se comprobó la integración de adjuntos desde Angular mediante acceso, carga, listado y descarga con un usuario radicador.

También utilicé ChatGPT para estructurar y revisar la redacción de los documentos de entrega. La asistencia de IA fue significativa y no se oculta; mi responsabilidad consistió en orientar las decisiones, revisar las soluciones propuestas, comprobar su funcionamiento y comprender el código entregado para poder explicarlo y defenderlo.

**Herramienta declarada:** ChatGPT. Si durante el trabajo se emplearon además instrucciones personalizadas, *skills*, agentes u otras configuraciones específicas, deben identificarse por su nombre y función antes de considerar definitiva esta declaración; no se atribuyen aquí herramientas o configuraciones no confirmadas.

## 8. ¿Podría implementar la solución en Django? ¿Cómo la plantearía?

Sí, podría plantear una implementación equivalente con **Django y Django REST Framework**, aunque distinguiría el diseño propuesto de una implementación efectivamente realizada. Mi experiencia principal en este proyecto está en Java y Spring Boot; trasladaría los conceptos de separación de responsabilidades, validaciones y seguridad a las convenciones del ecosistema Python.

Modelaría entidades para BRP, oferta, oferente, pago, adjunto, usuario, módulo y asignación de módulos. Utilizaría PostgreSQL y migraciones de Django para el esquema y la carga inicial del catálogo. Los serializadores atenderían validaciones de estructura, mientras que una capa de servicios transaccionales aplicaría las reglas de negocio, como la suma de participaciones, la correspondencia de pagos y las transiciones de estado.

Para la autenticación evaluaría JWT con una librería mantenida y permisos personalizados por rol y módulo. El flujo de estados tendría validaciones explícitas en el servidor. Para concurrencia, Django no reproduce automáticamente el comportamiento de `@Version` de JPA: implementaría un campo de versión con actualización condicional o evaluaría `select_for_update()` dentro de `transaction.atomic()` según las necesidades de contención.

Los archivos se gestionarían con el sistema de almacenamiento de Django, conservando sus metadatos asociados a cada oferta. Finalmente, escribiría pruebas para reglas, permisos, transiciones y conflictos, y empaquetaría el entorno con Docker Compose.

---

**Alcance de este documento:** refleja las decisiones y comprobaciones conocidas de la demo. Las mejoras descritas no deben interpretarse como funcionalidades ya implementadas. Antes de publicar, confirmar las herramientas/configuraciones de IA utilizadas y que las credenciales de demostración y pasos del README correspondan al entorno de entrega.
