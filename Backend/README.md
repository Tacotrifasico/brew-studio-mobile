# Backend compartido · Cupa

## Estado verificado: 19 octubre 2026

Proyecto autorizado: `qndojncusqmhoxnokgwb` (Cupa). Se confirmó vacío antes del
despliegue: cero tablas públicas, cero usuarios, historial de migraciones vacío.
Ahora tiene **23 tablas públicas, todas con RLS**, sin permisos de tabla anónimos,
sin tablas ni funciones de IA. El asesor de seguridad de Supabase terminó sin
hallazgos. No equivale a una auditoría exhaustiva ni a una prueba entre teléfonos.

Las claves están en configuración local ignorada por Git. Nunca copiar una
`service_role`, clave secreta o contraseña a las apps. La publishable key se manda
en `apikey`; `Authorization: Bearer` contiene el JWT de la persona, no esa clave.

### Migraciones reales de ESTE proyecto

1. `20261019221912_initial_no_ai_schema.sql`
2. `20261019221935_permissions_and_ownership.sql`
3. `20261019222024_private_alias_lookup.sql`

`Backend/supabase/migrations` es la fuente de verdad del nuevo proyecto compartido.
Se generaron con Supabase CLI oficial **2.121.0**, se probaron en PostgreSQL efímero
y se desplegaron mediante el conector administrativo autorizado. Los prefijos se
reconciliaron con las versiones que ese conector registró en el servidor.
La primera migración consolida los contratos históricos, omitiendo los artefactos
de IA. La segunda cierra permisos y propiedad; la tercera deja la implementación
privilegiada de búsqueda exacta de alias en el esquema privado, no en la API.

**No ejecutar otra vez `supabase/migrations` de la raíz ni `001_brew_studio_schema.sql`
de Android sobre este proyecto.** Esos archivos son historia/ensayos de actualización,
no una segunda cola que deba aplicarse después del baseline. Tampoco editar o borrar
el historial remoto para intentar sincronizarlo. Las próximas migraciones se crean
con `supabase migration new <nombre> --workdir Backend` y se revisan antes de aplicar.
No usar `db reset` contra un servidor con datos. No existe otro proyecto Staging
aislado todavía; no confundir una configuración de compilación con otro servidor.

## Datos y privacidad

- Cuenta/perfil: `auth.users` y `profiles`. El alta crea perfil privado por defecto.
  La metadata sólo propone nombre/alias; nunca permisos. Alias inválido o duplicado
  recibe un alias de respaldo para no romper el registro.
- Inventario: `beans`, `grinders`, `equipment`; café y ajustes por método viven en
  `beans.brew_profiles` como JSONB (temperatura precisa en Celsius). `coffee_beans`
  es un espejo interno para referencias iOS: el cliente no puede escribirlo.
- Recetas: `recipes`, `recipe_ingredients`, `recipe_steps`.
- Técnicas: `techniques`, `technique_steps`.
- Preparación/cata: `brew_sessions`, `tastings`, `tasting_observations`, `cup_sessions`.
  Una taza puede existir sin cata. Los campos precisos no fuerzan redondeo a °C entero.
- Laboratorio: `lab_experiments`.
- Comunidad: `shares`, `share_likes`, `share_saves`, `inbox_items`, `activity_log`,
  `blocked_users`, `content_reports`. `brew_shares` queda sellada por compatibilidad;
  las apps actuales usan `shares`, no ambas tablas.

El inventario y los hijos de recetas/técnicas son privados incluso si hay una
publicación. Compartir entrega un snapshot separado, no acceso al inventario.
Los envíos directos sólo los leen emisor y destinatario. Los bloqueos se respetan
en ambos sentidos sin exponer la lista privada de bloqueos. Volver privado un perfil
oculta también sus publicaciones públicas anteriores. Guardados y reportes son
privados; un usuario no puede moderar sus propios reportes. El buzón sólo permite
al destinatario editar `read_at`. Las referencias entre entidades exigen mismo dueño.
`resolve_profile_alias(alias_input)` resuelve un alias exacto con sesión, no enumera
perfiles y no revela correos. Implementación privilegiada en `private`, API invoker.

## Pruebas reproducibles

Desde `Tools`: `npm run verify-production-backend` (PGlite 0.3.14 ya fijado).
Prueba instalación limpia y **31 comprobaciones** sin regalar permisos a los roles
de prueba: perfil automático, propietarios falsos, hijos ajenos, temperaturas
precisas, taza sin cata, público/directo, buzón, guardados, bloqueos, privacidad y
acceso anónimo. No sustituye las pruebas históricas de upgrades.

`verify-live-rls.sql` se ejecutó contra el proyecto real: aprobado. Simula tres
identidades con roles SQL; crea datos dentro de una transacción y hace **ROLLBACK**.
No manda correos ni deja cuentas ficticias. También comprueba eliminación de
destinatario sin romper las referencias. La consulta posterior confirmó cero
usuarios y cero perfiles de prueba. No es una prueba de Auth con JWT reales.

## Aún NO significa «apps completamente online»

Pendientes explícitos para ambas plataformas:

1. Configurar/verificar confirmación de correo, recuperación y redirects nativos.
   No se desactivó confirmación ni se contrató SMTP. No hay cuentas reales creadas.
2. Android: renovación/almacenamiento seguro de sesión, push/pull y outbox completos
   para inventario, recetas con hijos, tazas, catas y laboratorio. iOS conserva su
   coordinador, pero necesita ensayo real de sesión/conflictos y agregados completos.
3. Mapear `brew_profiles` en ambos clientes; la columna existe, pero los ajustes
   por grano/método todavía se conservan localmente y no deben anunciarse sincronizados.
4. Las cuatro RPC de importar/crear variante con padre+hijos **no están desplegadas**.
   Deben validar snapshots y realizar copia profunda transaccional; nunca éxito parcial.
5. Desplegar/verificar `delete-account` (no se desplegó ninguna Edge Function aquí).
   Storage de fotos/archivos tampoco se configuró ni se publicó un bucket abierto.
6. Dos cuentas reales, Android ↔ iPhone, registro/login, confirmación, intercambio,
   importación/variante, reinicio, offline, cambio de cuenta y conflictos.

No se cambió UI en esta fase ni se realizó validación visual física. Compilar los
clientes con la configuración local ya pasó en la fase anterior; eso no demuestra
que los flujos remotos pendientes funcionen. Ver `AXCIS_BACKEND_HANDOFF.md`.

## Operación

No quitar índices porque el asesor los marque «sin uso» en una base recién creada:
todavía no hay tráfico real para medirlos. Antes de una beta pública, revisar copias
de seguridad/restauración, límites del plan, SMTP, moderación y política de privacidad.
Mantener las dos apps contra el mismo contrato y evitar cambios destructivos.
