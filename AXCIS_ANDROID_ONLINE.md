# Axcis — pendientes Android para conexión online

La guía integral y el protocolo iPhone↔Android están en `AXCIS_BACKEND_HANDOFF.md` de la rama `ios-production-migration`.

Pendientes que bloquean declarar Android online:

- Configurar `.env` local con el mismo `SUPABASE_URL` y `SUPABASE_ANON_KEY` usados por Cupa Staging en iOS.
- Implementar refresh token, `expires_at`, renovación previa a llamadas y almacenamiento cifrado en `SessionManager.kt`/Auth.
- Mantener el logging HTTP sin cuerpos; nunca imprimir tokens.
- Reemplazar las constantes provisionales de receta en `SyncRepository.kt` por datos reales y sincronizar ingredientes/pasos, cambios, borrados y conflictos.
- Resolver envíos directos por alias y verificar muro, buzón, importación y variante contra las tablas canónicas de la rama iOS.
- Ejecutar el protocolo físico con dos cuentas y guardar UUID/capturas como evidencia.
- Las técnicas se guardan en Room y su biblioteca canónica es **Almacén → Técnicas**; Preparar, Laboratorio y Comunidad deben escribir/importar en esa misma tabla.

Buscar `AXCIS-ONLINE` en el proyecto para localizar los puntos P0 comentados en código.
