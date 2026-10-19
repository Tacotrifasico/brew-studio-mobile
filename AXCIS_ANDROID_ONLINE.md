# Axcis — pendientes Android para conexión online

BARC reversible: el contrato de ajustes por grano/método y su pendiente de sync
están en [BARC_BEAN_PROFILES.md](BARC_BEAN_PROFILES.md). Persistencia local lista;
no declarar sincronización entre teléfonos hasta conectar `beans.brew_profiles`.

La guía integral y el protocolo iPhone↔Android están en `AXCIS_BACKEND_HANDOFF.md` de la rama `ios-production-migration`.

Actualización 2026-10-19: Codex realizará la integración por solicitud del
propietario. `AXCIS-ONLINE` conserva su nombre histórico para localizar pendientes.
No integrar IA ni desplegar Gemini: los consejos del laboratorio son locales.
El proyecto Supabase creado todavía no demuestra que las apps estén online.

Servidor inicial desplegado (19 octubre 2026): proyecto `qndojncusqmhoxnokgwb`,
23 tablas públicas con RLS, sin permisos anónimos de tabla y sin artefactos de IA.
31 pruebas locales de contrato y ensayo SQL de roles A/B/C contra el servidor
aprobados; no quedaron usuarios ficticios. El asesor de seguridad no reportó hallazgos.
Es una base compartida, NO una afirmación de sincronización completa de las apps.
La fuente de verdad aplicada es `Backend/supabase/migrations` en la rama iOS;
consultar allí `Backend/README.md`. No reaplicar el SQL inicial Android ni la cola
histórica iOS de la raíz sobre este nuevo proyecto.
URL y clave pública ya están en `.env` local y en configuraciones iOS locales
ignoradas por Git. El cliente Android ya distingue publishable key (`apikey`) del
JWT de sesión (`Authorization: Bearer`), con pruebas. Falta el resto de esta lista.

Pendientes que bloquean declarar Android online:

- Configurar `.env` local con el mismo `SUPABASE_URL` y `SUPABASE_ANON_KEY` usados por Cupa Staging en iOS.
- Antes de aplicar SQL, confirmar si el proyecto está vacío o contiene datos, revisar las migraciones canónicas de la rama iOS y probar en Staging. URL/clave pública no permiten administrar el servidor: se necesita acceso autorizado mediante CLI o panel; nunca copiar credenciales administrativas al cliente ni al repositorio.
- Implementar refresh token, `expires_at`, renovación previa a llamadas y almacenamiento cifrado en `SessionManager.kt`/Auth.
- Mantener el logging HTTP sin cuerpos; nunca imprimir tokens.
- Conectar el contrato remoto canónico completo de recetas (`recipes`, `recipe_ingredients` y `recipe_steps`). El agregado local ya guarda padre, ingredientes y pasos de forma transaccional, conserva su orden y está cubierto por pruebas; `SyncRepository.kt` no lo sube mediante el contrato heredado porque hacerlo perdería hijos y fabricaría cantidades. Las recetas permanecen locales y pendientes hasta recibir confirmación del agregado remoto completo.
- Completar técnicas más allá del alta segura actual. Android puede crear la técnica, reintentar únicamente los pasos faltantes e importar agregados remotos completos; actualizaciones, borrados lógicos y conflictos remotos siguen esperando endpoints/contrato de Axcis. Nunca marcar la técnica `SYNCED` si falta un paso o si agua, proporción, acumulados y duración no coinciden.
- Ampliar `SyncRepository.kt` más allá de recetas y técnicas: `beans`, `instruments`/equipo, `cups`, `catas` y `lab_experiments` ya se guardan localmente con `PENDING_CREATE`/`PENDING_UPDATE`, pero todavía no tienen push/pull remoto en Android. No cambiar esos estados a `SYNCED` hasta recibir confirmación del servidor.
- Implementar borrado lógico y outbox para cafés, equipo, tazas, catas y experimentos. Hoy su eliminación local es inmediata porque el contrato Android aún no posee una cola de borrado para esas entidades.
- Integrar en Android los envíos directos por alias y verificar muro, buzón, importación y variante contra las tablas canónicas de la rama iOS.
  - La RPC real es `resolve_profile_alias(alias_input)` (no `resolve_profile_id_by_alias`). Ya está desplegada en el proyecto nuevo y probada con roles SQL; falta consumirla desde Android y probar con JWT reales. Mientras tanto el cliente identifica el campo como temporal, exige un UUID real y bloquea la publicación de recetas/técnicas que aún no tienen identidad remota. Compartir nunca debe cambiar por sí solo el `syncStatus` de la fórmula.
  - Los snapshots sociales Android ya incluyen ingredientes/pasos reales de recetas y pasos/acumulados reales de técnicas. La importación rechaza snapshots parciales y guarda padre e hijos localmente en una transacción. Implementar las cuatro RPC de copia/variante según la sección **Contrato obligatorio de `payload_snapshot_json`** de `AXCIS_BACKEND_HANDOFF.md`; una RPC no puede confirmar éxito si sólo creó el padre.
- Ejecutar el protocolo físico con dos cuentas y guardar UUID/capturas como evidencia.
- Las técnicas se guardan en Room y su biblioteca canónica es **Almacén → Técnicas**; Preparar, Laboratorio y Comunidad deben escribir/importar en esa misma tabla.
- `Cup.techniqueId` y `Cup.methodId` ya conservan la técnica y el método usados cuando la preparación proviene del Almacén; mantener esos UUID al mapear `cups` y `catas` al backend.

Buscar `AXCIS-ONLINE` en el proyecto para localizar los puntos P0 comentados en código.

## Evidencia que no depende de Axcis

- Recetas locales: padre, ingredientes y pasos ordenados se guardan juntos; editar reemplaza el agregado sin dejar hijos obsoletos; decimales con coma se normalizan.
- Técnicas locales: agua total, proporción, duración y acumulados se validan antes de guardar; cada técnica obtiene UUID propios para sus pasos.
- Laboratorio → Técnica → Almacén → Preparar → Cata: recorrido automático aprobado con identidad y snapshots conservados.
- La pantalla Perfil ya no afirma que Supabase o RLS están operativos: muestra modo local, sincronización parcial y prueba A/B pendiente según la configuración real.

## Evidencia que sí requiere a Axcis o infraestructura externa

- URL y clave pública del mismo proyecto Supabase Staging en ambos clientes.
- Migraciones canónicas desplegadas, Auth/SMTP/redirects y Edge Functions configuradas.
- Push/pull remoto completo de todos los agregados Android y refresh seguro de sesión.
- Ensayo físico con dos cuentas/JWT reales para RLS, intercambio Android↔iPhone, alias, muro y buzón.
