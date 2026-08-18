# Migraciones Supabase

Referencia actual: `supabase/migrations/001_brew_studio_schema.sql` del proyecto Android.

Antes de producción se requiere:

1. Auditar tablas, claves foráneas, índices y funciones RPC existentes.
2. Añadir migraciones nuevas e idempotentes; no modificar una migración ya aplicada.
3. Validar RLS con usuarios A/B para lectura y escritura privada.
4. Mantener contenido público/social separado de datos privados.
5. Validar borrado lógico, cascadas y restauración.
6. Implementar Edge Function para Gemini con autenticación y rate limit.

Nunca se incluye `service_role` en iOS.
