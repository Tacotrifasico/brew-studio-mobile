# Migraciones Supabase

Referencia original: `supabase/migrations/001_brew_studio_schema.sql` del proyecto Android.

La migración idempotente `supabase/migrations/202608170001_ios_core_schema.sql` prepara perfiles, inventario, recetas, técnicas, sesiones de preparación, catas, observaciones, tazas y el registro de cuota de IA. Incluye UUID estables, propiedad, fechas, versión, borrado lógico, claves foráneas, restricciones, índices y políticas RLS `owner_id = auth.uid()` para todas las entidades privadas.

Antes de producción se requiere:

1. Comparar esta migración con el esquema ya desplegado antes de aplicarla; no modificar una migración que ya haya sido ejecutada.
2. Validar RLS con usuarios A/B para lectura y escritura privada.
3. Mantener contenido público/social separado de datos privados.
4. Validar borrado lógico, cascadas y restauración.
5. Implementar Edge Function para eliminación de cuenta y otra para Gemini con autenticación y rate limit.

Nunca se incluye `service_role` en iOS.
