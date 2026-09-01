# Migraciones Supabase

Referencia original: `supabase/migrations/001_brew_studio_schema.sql` del proyecto Android.

La migración idempotente `supabase/migrations/202608170001_ios_core_schema.sql` prepara perfiles, inventario, recetas, técnicas, sesiones de preparación, catas, observaciones, tazas y el registro de cuota de IA. Incluye UUID estables, propiedad, fechas, versión, borrado lógico, claves foráneas, restricciones, índices y políticas RLS `owner_id = auth.uid()` para todas las entidades privadas.

La migración `supabase/migrations/202608170002_social_content_moderation.sql` agrega límites de longitud y un filtro preventivo de texto en `brew_shares`. El trigger revisa identidad pública, título, mensaje y todo el snapshot JSON en cada alta o modificación, de modo que una llamada directa a REST no pueda saltarse la misma política aplicada por el cliente iOS.

La migración `supabase/migrations/202608170003_lab_and_brew_references.sql` añade las referencias de método, receta, técnica, café y molino a los experimentos, y método más snapshot de receta a las sesiones. Es idempotente, incorpora claves foráneas `ON DELETE SET NULL` e índices; los snapshots permanecen aunque una referencia sea eliminada físicamente.

La migración `supabase/migrations/202608170004_cup_history_snapshots.sql` amplía `cup_sessions` de forma idempotente con las referencias, parámetros ejecutados, nombres congelados, vida térmica, valoración, comentario, fecha y snapshots JSON presentes en Android. Añade claves foráneas `ON DELETE SET NULL` e índices de consulta sin modificar migraciones ya desplegadas.

La migración `supabase/migrations/202608180005_social_inbox_activity.sql` agrega el buzón directo y la cronología existentes en Android. Un trigger `SECURITY DEFINER` crea o retira la entrega al cambiar una publicación directa, y rellena publicaciones directas previas de forma idempotente. RLS permite al destinatario consultar su bandeja y modificar únicamente `read_at`; la actividad sólo puede insertarse y leerse con `user_id = auth.uid()`.

La migración `supabase/migrations/202609010006_profile_and_ugc_hardening.sql` añade restricciones idempotentes para identidad, biografía, color y reportes. También impide en el servidor que un perfil privado publique en el muro, aunque alguien intente omitir la regla del cliente y llamar REST directamente. Las restricciones se agregan como `NOT VALID`: protegen altas y cambios nuevos sin bloquear el despliegue por datos históricos; antes de validarlas globalmente se deben auditar y normalizar las filas antiguas.

Antes de producción se requiere:

1. Comparar esta migración con el esquema ya desplegado antes de aplicarla; no modificar una migración que ya haya sido ejecutada.
2. Validar RLS con usuarios A/B para lectura y escritura privada.
3. Mantener contenido público/social separado de datos privados.
4. Validar borrado lógico, cascadas y restauración.
5. Desplegar y validar las Edge Functions de eliminación de cuenta y Gemini con autenticación y rate limit.
6. Probar el filtro social, privacidad de perfil, bloqueo reversible y categorías de reporte, además del flujo humano de respuesta a reportes.
7. Validar con dos usuarios que una entrega directa sólo aparece al destinatario y que otro usuario no puede marcarla como leída.

Nunca se incluye `service_role` en iOS.
