# Migraciones Supabase

Referencia original: `supabase/migrations/001_brew_studio_schema.sql` del proyecto Android.

`supabase/migrations/202608160000_android_schema_preflight.sql` se ejecuta primero cuando ya existe el esquema Android. Sólo agrega las columnas que las migraciones iOS posteriores necesitan y rellena `owner_id` desde `user_id`; no renombra ni elimina tablas o datos.

La migración idempotente `supabase/migrations/202608170001_ios_core_schema.sql` prepara perfiles, inventario, recetas, técnicas, sesiones de preparación, catas, observaciones, tazas y el registro de cuota de IA. Incluye UUID estables, propiedad, fechas, versión, borrado lógico, claves foráneas, restricciones, índices y políticas RLS `owner_id = auth.uid()` para todas las entidades privadas.

La migración `supabase/migrations/202608170002_social_content_moderation.sql` agrega límites de longitud y un filtro preventivo de texto en `brew_shares`. El trigger revisa identidad pública, título, mensaje y todo el snapshot JSON en cada alta o modificación, de modo que una llamada directa a REST no pueda saltarse la misma política aplicada por el cliente iOS.

La migración `supabase/migrations/202608170003_lab_and_brew_references.sql` copia primero los granos Android al espejo relacional `coffee_beans`, antes de crear claves foráneas. Después añade las referencias de método, receta, técnica, café y molino a los experimentos, y método más snapshot de receta a las sesiones. Es idempotente, incorpora claves foráneas `ON DELETE SET NULL` e índices; los snapshots permanecen aunque una referencia sea eliminada físicamente.

La migración `supabase/migrations/202608170004_cup_history_snapshots.sql` amplía `cup_sessions` de forma idempotente con las referencias, parámetros ejecutados, nombres congelados, vida térmica, valoración, comentario, fecha y snapshots JSON presentes en Android. Añade claves foráneas `ON DELETE SET NULL` e índices de consulta sin modificar migraciones ya desplegadas.

La migración `supabase/migrations/202608180005_social_inbox_activity.sql` agrega el buzón directo y la cronología existentes en Android. Un trigger `SECURITY DEFINER` crea o retira la entrega al cambiar una publicación directa, y rellena publicaciones directas previas de forma idempotente. RLS permite al destinatario consultar su bandeja y modificar únicamente `read_at`; la actividad sólo puede insertarse y leerse con `user_id = auth.uid()`.

La migración `supabase/migrations/202609010006_profile_and_ugc_hardening.sql` añade restricciones idempotentes para identidad, biografía, color y reportes. También impide en el servidor que un perfil privado publique en el muro, aunque alguien intente omitir la regla del cliente y llamar REST directamente. Las restricciones se agregan como `NOT VALID`: protegen altas y cambios nuevos sin bloquear el despliegue por datos históricos; antes de validarlas globalmente se deben auditar y normalizar las filas antiguas.

`supabase/migrations/202609010007_android_backend_alignment.sql` convierte el esquema Android en el contrato compartido definitivo. Extiende `beans`, `grinders`, `equipment`, `recipes`, `techniques`, `technique_steps`, `lab_experiments`, `profiles` y `shares` sin borrar columnas legadas; sincroniza pares de campos en ambas direcciones; migra publicaciones iOS previas a `shares`; reconecta likes, guardados, reportes, buzón y actividad; y reemplaza las políticas públicas demasiado amplias por RLS autenticada y aislada. `beans` es la fuente de verdad del café y mantiene un espejo automático en `coffee_beans` sólo para las relaciones históricas iOS. Los snapshots sociales remotos son planos, como Android, y el límite común de mensaje es 280 caracteres.

Las ocho migraciones fueron analizadas sintácticamente como PostgreSQL con `pglast 7.7` y ejecutadas completas sobre PostgreSQL efímero mediante `Tools/BackendMigrationVerifier.mjs`. El verificador cubre dos historiales: esquema Android con datos de cada tabla compartida y esquema iOS previo con café que debe conservarse; después comprueba propagación Android→iOS, iOS→Android y RLS de lectura/escritura con dos UUID sintéticos. Esta validación no sustituye un ensayo sobre un clon del proyecto real ni la prueba con dos sesiones JWT reales.

Antes de producción se requiere:

1. Crear un respaldo y probar la secuencia completa sobre un clon de Staging del esquema Android antes de producción; no aplicar cambios destructivos sin revisar las filas afectadas.
2. Validar RLS con usuarios A/B para lectura y escritura privada.
3. Mantener contenido público/social separado de datos privados.
4. Validar borrado lógico, cascadas y restauración.
5. Desplegar y validar las Edge Functions de eliminación de cuenta y Gemini con autenticación y rate limit.
6. Probar el filtro social, privacidad de perfil, bloqueo reversible y categorías de reporte, además del flujo humano de respuesta a reportes.
7. Validar con dos usuarios que una entrega directa sólo aparece al destinatario y que otro usuario no puede marcarla como leída.
8. Comprobar Android → iOS e iOS → Android para café, molino, equipo, receta, técnica, pasos, laboratorio, perfil y publicación social.

Nunca se incluye `service_role` en iOS.
