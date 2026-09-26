# Entrega online para Axcis

Esta es la guía operativa para conectar Android e iOS al mismo backend y validar el núcleo social de Cupa. Los marcadores `AXCIS-ONLINE` dentro del código señalan puntos que no deben darse por terminados sin la validación indicada aquí.

## Fuente de verdad

- Repositorio: `Tacotrifasico/brew-studio-mobile`.
- Android vigente: rama `main`.
- iOS vigente: rama `ios-production-migration`.
- Contrato canónico: `profiles`, `beans`, `grinders`, `equipment`, `recipes`, `recipe_ingredients`, `recipe_steps`, `techniques`, `technique_steps`, `shares`, `share_likes`, `share_saves`, `inbox_items`, `activity_log`, `blocked_users` y `content_reports`.
- Migraciones canónicas: `supabase/migrations` de `ios-production-migration`, en orden lexicográfico. No aplicar únicamente el SQL inicial de Android sobre producción.
- Identidad compartida: `auth.users.id`; propiedad privada mediante `user_id`; UUID estable por entidad.
- Las técnicas pertenecen al **Almacén**. Preparar y Laboratorio son puntos de creación/uso, no almacenes alternos.

## P0 — necesario para estar online

1. Crear o clonar un proyecto **Supabase Staging** y obtener `PROJECT_REF`, URL HTTPS y publishable/anon key.
2. Crear respaldo y ejecutar todas las migraciones con `supabase db push` en Staging.
3. Configurar Auth Email, SMTP, confirmación de correo y redirects:
   - `com.tacotrifasico.cupa://auth/recovery`
   - `com.tacotrifasico.cupa.staging://auth/recovery`
4. Desplegar `gemini-suggestions` y `delete-account`; `GEMINI_API_KEY` vive únicamente como secreto de la Edge Function.
5. Android: completar refresh token, expiración y almacenamiento cifrado señalados en `SessionManager.kt`. Eliminar el estado falso de “conectado” cuando el JWT venció.
6. Android: implementar el contrato canónico de recetas con `recipe_ingredients` y `recipe_steps`. `SyncRepository.kt` conserva hoy las recetas localmente en lugar de enviarlas por el contrato heredado, porque ese contrato perdería los hijos y obligaría a inventar cantidades.
   - Las técnicas ya permiten alta segura, reintento de pasos faltantes e importación de agregados remotos completos. Falta implementar actualizaciones, borrados lógicos y resolución de conflictos; no declarar `SYNCED` un agregado incompleto.
   - Incluir también `beans`, `instruments`/equipo, `cups`, `catas` y `lab_experiments`. Android ya marca las escrituras locales como pendientes y conserva `Cup.techniqueId`/`Cup.methodId`; falta que el backend confirme cada operación antes de pasarla a `SYNCED`.
   - Agregar outbox y borrado lógico para esas entidades. Su borrado Android sigue siendo únicamente local hasta que exista ese contrato remoto.
7. Confirmar que Android e iOS usan el mismo Staging:
   - Android: `.env` local con `SUPABASE_URL` y `SUPABASE_ANON_KEY`.
   - iOS: configuración Staging mediante xcconfig local con las mismas dos claves.
8. Validar RLS con dos usuarios reales antes de usar producción.

## P1 — núcleo social antes de beta pública

- Resolver destinatarios por alias; el usuario no debe copiar UUID.
- Muro público: publicar receta y técnica, ver autor, atribución y snapshot completo.
- Envío directo: crear `share` de visibilidad `direct`, generar `inbox_item` y permitir al receptor marcarlo leído.
- Importar crea una copia profunda con UUID nuevos y `copy_mode=IMPORT`.
- Variante crea una copia profunda con UUID nuevos y `copy_mode=FORK`.
- Conservar siempre `original_author_user_id`, `original_author_name`, `original_entity_id`, `root_entity_id` e `imported_from_share_id`.
- Likes idempotentes, guardados privados, bloqueo reversible y reporte moderable.
- Actualización, borrado lógico, reintento offline y conflicto por `updated_at`/versión deben funcionar en ambas plataformas.

## Configuración segura

Nunca subir `service_role`, `GEMINI_API_KEY`, contraseñas, access tokens ni refresh tokens. Sí pueden vivir en los clientes la URL pública y la publishable/anon key. Desactivar registro de cuerpos HTTP: una respuesta de Auth contiene tokens.

## Ensayo físico iPhone ↔ Android

Usar dos correos diferentes; no iniciar la misma cuenta en ambos teléfonos para esta prueba.

### Preparación

1. Instalar **Cupa Staging** en iPhone y el APK `debug` conectado al mismo Staging en Android.
2. Crear usuario A en Android con alias `@android_a`.
3. Crear usuario B en iPhone con alias `@iphone_b`.
4. Confirmar ambos correos, cerrar y volver a abrir las apps, e iniciar sesión otra vez.
5. En Supabase verificar dos filas distintas en `profiles` y que ninguna pueda leer inventario privado de la otra.

### Android → iPhone

1. En Android crear una receta y una técnica de tres pasos desde **Almacén**.
2. Confirmar que la técnica aparece en **Almacén → Técnicas**.
3. Publicar la receta en el muro. En iPhone actualizar Comunidad y abrirla.
4. Importarla en iPhone. Debe aparecer en **Almacén → Recetas** y poder editarse sin modificar la original.
5. Enviar la técnica directamente a `@iphone_b`.
6. En iPhone abrir Recibidos, marcarla leída e importarla. Debe aparecer en **Almacén → Técnicas**, conservar sus pasos y poder iniciar Preparar.

### iPhone → Android

Repetir el recorrido al revés con nombres diferentes. En Android la receta importada debe aparecer en Recetas y la técnica en **Almacén → Técnicas**. Crear además una variante y comprobar que el autor original sigue visible.

### Reinicio y aislamiento

1. Apagar red, editar una técnica en cada teléfono y recuperar conexión; comprobar sincronización o conflicto explícito.
2. Forzar cierre y reabrir: la sesión debe renovarse o pedir login, nunca mostrar una comunidad vacía fingiendo estar conectada.
3. Cerrar sesión A e iniciar B en el mismo teléfono: no deben verse datos privados de A.
4. Intentar leer por REST una fila privada del otro usuario: RLS debe devolver cero filas o 403.
5. Interrumpir una descarga entre `techniques` y `technique_steps`: iOS debe conservar el agregado local anterior y Android no debe importar la técnica nueva. Al reintentar, sólo debe aparecer cuando todos los pasos y acumulados sean válidos.

## Evidencia obligatoria

Guardar fecha, SHA de ambas apps, UUID de usuarios/receta/técnica/shares, capturas de Almacenes y buzones, resultado offline/reinicio/RLS, conteos de migración y errores con responsable. La salida se acepta cuando una receta y una técnica con pasos completan el recorrido en ambos sentidos y sobreviven reinicio, reinstalación/login y cambio de cuenta.
