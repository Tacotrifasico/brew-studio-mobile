# Axcis — pendientes Android para conexión online

La guía integral y el protocolo iPhone↔Android están en `AXCIS_BACKEND_HANDOFF.md` de la rama `ios-production-migration`.

Pendientes que bloquean declarar Android online:

- Configurar `.env` local con el mismo `SUPABASE_URL` y `SUPABASE_ANON_KEY` usados por Cupa Staging en iOS.
- Implementar refresh token, `expires_at`, renovación previa a llamadas y almacenamiento cifrado en `SessionManager.kt`/Auth.
- Mantener el logging HTTP sin cuerpos; nunca imprimir tokens.
- Reemplazar las constantes provisionales de receta en `SyncRepository.kt` por datos reales y sincronizar ingredientes/pasos, cambios, borrados y conflictos.
- Ampliar `SyncRepository.kt` más allá de recetas y técnicas: `beans`, `instruments`/equipo, `cups`, `catas` y `lab_experiments` ya se guardan localmente con `PENDING_CREATE`/`PENDING_UPDATE`, pero todavía no tienen push/pull remoto en Android. No cambiar esos estados a `SYNCED` hasta recibir confirmación del servidor.
- Implementar borrado lógico y outbox para cafés, equipo, tazas, catas y experimentos. Hoy su eliminación local es inmediata porque el contrato Android aún no posee una cola de borrado para esas entidades.
- Resolver envíos directos por alias y verificar muro, buzón, importación y variante contra las tablas canónicas de la rama iOS.
- Ejecutar el protocolo físico con dos cuentas y guardar UUID/capturas como evidencia.
- Las técnicas se guardan en Room y su biblioteca canónica es **Almacén → Técnicas**; Preparar, Laboratorio y Comunidad deben escribir/importar en esa misma tabla.
- `Cup.techniqueId` y `Cup.methodId` ya conservan la técnica y el método usados cuando la preparación proviene del Almacén; mantener esos UUID al mapear `cups` y `catas` al backend.

Buscar `AXCIS-ONLINE` en el proyecto para localizar los puntos P0 comentados en código.
