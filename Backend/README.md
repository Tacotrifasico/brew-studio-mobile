# Cupa · backend mínimo, biblioteca local

## Alcance vigente — 19 octubre 2026

La biblioteca vive en Room (Android) y Core Data/SQLite (iOS). Guardar, editar,
preparar, catar y abrir la app **no sube el Almacén**. No hay respaldo de la
biblioteca en Supabase. Perder/desinstalar el almacenamiento local puede perder
esos datos: exportar archivos antes; los respaldos del sistema operativo son
independientes y no son un servicio de Cupa.

La nube sólo mantiene cuentas/perfiles y comunidad: publicaciones explícitas de
recetas/técnicas, registro de creadores, likes, guardados, recibidos y seguridad
social (bloqueos/reportes). Importar o crear variante guarda una copia profunda
local con UUID nuevos, pasos completos y atribución; NO crea otra biblioteca remota.
No implementar las antiguas RPC de copia remota: contradicen este alcance.

## Despliegue real

Proyecto: `qndojncusqmhoxnokgwb`. Fuente única de migraciones:
`Backend/supabase/migrations` en `ios-production-migration`.

1. `20261019221912_initial_no_ai_schema.sql`
2. `20261019221935_permissions_and_ownership.sql`
3. `20261019222024_private_alias_lookup.sql`
4. `20261019224346_local_first_community.sql`
5. `20261019231331_snapshot_type_contract.sql`

Las primeras tres prepararon el esquema general; la cuarta cierra toda API de
biblioteca privada sin borrar tablas ni datos. Existen 24 tablas públicas con
RLS; sólo nueve tienen uso de cuenta/comunidad: `profiles`, `shares`,
`community_creations`, `share_likes`, `share_saves`, `inbox_items`,
`activity_log`, `blocked_users` y `content_reports`.
Las 14 tablas de inventario/preparación quedan selladas para clientes;
`brew_shares` es legado sellado. No hay buckets, Realtime ni IA activados por
esta integración. No se creó otro proyecto ni se cambió de plan.

No reaplicar `supabase/migrations` de la raíz ni el SQL inicial Android:
son historial de upgrades, no una segunda cola tras este baseline.
CLI oficial usado: 2.121.0. Generar cambios con
`supabase migration new <nombre> --workdir Backend`. Los prefijos locales se
reconciliaron con las versiones reales del conector administrativo.
No usar reset, borrar tablas o reescribir migraciones ya aplicadas.

## Intercambio y autoría

`shares.entity_id` identifica el registro LOCAL de origen: no necesita fila en
`recipes`/`techniques` remotas. El snapshot es plano y autónomo; no contiene
granos, equipo, catas ni el inventario. Cada receta incluye ingredientes/pasos;
cada técnica incluye cantidades, tiempos, acumulados y ajustes de preparación.

El servidor valida el agregado, deriva publicador/alias desde su perfil y registra
una creación inmutable en `community_creations`. No confía en el nombre/autor que
mande el cliente. Una copia compartida incluye `source_share_id`; el servidor
comprueba acceso al origen y conserva el creador original. El publicador de una
variante sigue identificado por su propia cuenta. No puede falsificarse otro
creador sin una publicación accesible. Si el origen fue retirado o bloqueado,
volver a publicarlo puede rechazarse; la copia local no se borra.

Los likes son idempotentes, con contador mantenido por trigger. La identidad de
quienes dieron like no se expone a otros usuarios. Un envío directo no aparece a
terceros. Bloqueos simétricos, perfiles privados, reportes privados y buzón con
edición restringida a `read_at` siguen protegidos por RLS y grants explícitos.

## Contención de consumo

- Sólo publicación explícita; nunca sincronización de biblioteca al ingresar.
- Máximo 64 KiB de snapshot, 80 pasos/ingredientes y 30 publicaciones por hora/cuenta.
- Consultas de comunidad acotadas a 30 elementos por carga; no descarga ilimitada.
- Likes con índice único y contador: no descargar todas las personas para contarlos.
- No fotos/archivos en Storage, suscripciones Realtime ni servicios de generación.
- Las copias importadas y los ajustes BARC por grano/método permanecen locales.

Estas medidas NO son un límite monetario del proveedor. Antes de beta pública
revisar tráfico real, tamaño de snapshots, retención, límites/alertas del plan y SMTP.
No prometer costo cero o un importe fijo sin medir usuarios y transferencia.

## Evidencia y pendientes reales

`npm run verify-production-backend` desde Tools: 31 pruebas del baseline histórico
más 33 del alcance local-first, sin regalar grants a roles de prueba.
`verify-live-rls.sql`: aprobado contra el servidor real con roles A/B/C;
todos los usuarios/publicaciones de prueba se revierten con ROLLBACK, sin emails.
Asesor de seguridad sin hallazgos tras la migración.

iOS sólo encola/transfiere `profiles`; operaciones antiguas de inventario quedan
intactas, no se envían ni se marcan sincronizadas. Android no sube/descarga técnicas
privadas; publicar acepta el UUID local. Los estados internos heredados PENDING no
implican una futura subida ni se muestran como error de almacenamiento.

Pendiente operativo, no confundir con un resultado probado:
- Auth: cinco redirects nativos exactos ya guardados con aprobación del propietario,
  sin localhost ni comodines. Falta proveedor/remitente SMTP. No se desactivó
  confirmación ni se contrató un servicio. La sesión del panel caducó tras revisar SMTP.
- Registro/login real con correos confirmados, JWT renovado y prueba en dos teléfonos.
  No se crearon contraseñas ni se enviaron correos al propietario.
- Función delete-account desplegada (SDK 2.118.0 fijado): rechaza sin JWT o JWT inválido con 401. Falta ensayo autenticado de eliminación antes de beta pública.
- No hay validación visual física de esta fase; compilar no demuestra UX real.
