# Migraciones Supabase

Referencia original: `supabase/migrations/001_brew_studio_schema.sql` del proyecto Android.

La migración idempotente `supabase/migrations/202608170001_ios_core_schema.sql` prepara perfiles, inventario, recetas, técnicas, sesiones de preparación, catas, observaciones, tazas y el registro de cuota de IA. Incluye UUID estables, propiedad, fechas, versión, borrado lógico, claves foráneas, restricciones, índices y políticas RLS `owner_id = auth.uid()` para todas las entidades privadas.

La migración `supabase/migrations/202608170002_social_content_moderation.sql` agrega límites de longitud y un filtro preventivo de texto en `brew_shares`. El trigger revisa identidad pública, título, mensaje y todo el snapshot JSON en cada alta o modificación, de modo que una llamada directa a REST no pueda saltarse la misma política aplicada por el cliente iOS.

La migración `supabase/migrations/202608170003_lab_and_brew_references.sql` añade las referencias de método, receta, técnica, café y molino a los experimentos, y método más snapshot de receta a las sesiones. Es idempotente, incorpora claves foráneas `ON DELETE SET NULL` e índices; los snapshots permanecen aunque una referencia sea eliminada físicamente.

Antes de producción se requiere:

1. Comparar esta migración con el esquema ya desplegado antes de aplicarla; no modificar una migración que ya haya sido ejecutada.
2. Validar RLS con usuarios A/B para lectura y escritura privada.
3. Mantener contenido público/social separado de datos privados.
4. Validar borrado lógico, cascadas y restauración.
5. Desplegar y validar las Edge Functions de eliminación de cuenta y Gemini con autenticación y rate limit.
6. Probar el filtro social con texto permitido y no permitido, además del flujo humano de respuesta a reportes.

Nunca se incluye `service_role` en iOS.
