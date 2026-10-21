# Cupa · entrega de cuentas y comunidad

## Contrato vigente

El propietario cambió el alcance el 19 octubre 2026: **biblioteca local, nube
sólo para cuentas e intercambio social**. Este documento reemplaza los pendientes
anteriores de subir todo el Almacén. No habilitar esa sincronización ni las cuatro
RPC antiguas de importar/fork remoto.

- Android: rama `main`, Room.
- iOS: rama `ios-production-migration`, Core Data/SQLite.
- Backend compartido: `qndojncusqmhoxnokgwb`.
- Fuente de verdad del servidor: [Backend/README.md](Backend/README.md) y
  `Backend/supabase/migrations`. No reaplicar la cola histórica de la raíz.
- No Gemini/IA. Altura y unidades en Configuración; temperaturas internas en Celsius.
- Firma y configuración personal de Xcode no se deben sobrescribir.

## Qué se queda en el teléfono

Granos y ajustes BARC por método, molinos, equipo/métodos, biblioteca de recetas
e ingredientes/pasos, técnicas/pasos, experimentos, preparaciones, tazas y catas.
Guardar/editar/importar no llama al servidor para respaldar esos registros.
No anunciar recuperación de esa biblioteca al reinstalar o cambiar de teléfono.
Los archivos exportados y respaldos del sistema operativo son independientes.

## Qué usa Supabase

Auth y perfiles, snapshots que el usuario decide publicar o enviar, registro del
creador, likes/contador, guardados, recibidos, actividad social mínima, bloqueos y
reportes. Sin buckets ni conexiones Realtime permanentes.
Guardar localmente no genera solicitudes ni filas en esas tablas.

La publicación usa el UUID local como `entity_id`, sin exigir una receta remota.
El servidor registra al creador y deriva identidad del perfil autenticado.
Importar/crear variante usa snapshot validado, UUID nuevos y transacción local.
La copia conserva autor, entidad original y publicación de origen.
Al volver a compartir se manda `source_share_id`; la autoría no se inventa.

## Seguridad y consumo

La API de las tablas privadas está revocada; no borrar las tablas vacías como
mecanismo de ahorro. Sólo las nueve tablas sociales/de perfil están operativas.
No secrets/service_role en clientes. Publishable key en `apikey`; JWT del usuario
en Bearer. Los archivos de configuración local están ignorados por Git.
Android cifra credenciales con Keystore, conserva refresh/expiración, renueva
antes de Comunidad y no respalda credenciales por copia/transferencia del SO.
iOS mantiene Keychain y renovación. Una confirmación de registro pendiente no
se trata como sesión iniciada.

Límites: 64 KiB por snapshot, 80 pasos/ingredientes, 30 publicaciones/hora/cuenta,
30 resultados por carga. No es garantía de factura: revisar uso y límites del plan.

## Ensayo final con dos cuentas (no realizado todavía)

1. Verificar Auth Email/SMTP y redirects desde el panel autenticado.
   Recuperación iOS: `com.tacotrifasico.cupa://auth/recovery` y
   `com.tacotrifasico.cupa.staging://auth/recovery`.
   No desactivar confirmación para evitar configurar correo.
2. Instalar las compilaciones actuales en ambos teléfonos; registrarse con dos
   correos propios, elegir contraseñas en los dispositivos y confirmar correos.
3. Perfil A: nombre/alias y público si desea publicar al muro. Perfil B puede
   permanecer privado para envíos directos.
4. Android crea receta y técnica en Almacén; antes de compartir verificar que
   no existen filas privadas remotas. Publicar receta y enviar técnica a @alias B.
5. iPhone abre muro/Recibidos, importa y crea variante. Verificar hijos, cantidades,
   acumulados y autor. Editar la copia NO modifica el original.
6. Repetir iPhone → Android. Dar/quitar like; tras recargar y reabrir, estado y
   total correctos. Enviar dos veces el mismo like no incrementa el total.
7. Republicar una variante conserva creador original y publicador distinto.
8. Sin conexión: inventario, preparación, laboratorio, cata e importados funcionan.
   Reinicio/cambio de cuenta no mezcla bibliotecas privadas.
9. Confirmar que compartir no sube inventario/equipo/tazas y que ninguna cuenta
   puede leer envíos directos ajenos. Bloqueo se respeta en ambos sentidos.

## Evidencia actual y bloqueos operativos

31 pruebas locales del baseline + 33 local-first y ensayo SQL real con roles
aprobados. Todos los fixtures se revierten; cero usuarios reales creados por esas
pruebas, sin correos enviados. Esto no sustituye JWT reales ni teléfonos.
El asesor de seguridad terminó sin hallazgos. Compilación, lógica y validación
visual deben informarse por separado.

El panel se revisó con sesión del propietario. Con su aprobación se reemplazó
localhost por com.tacotrifasico.cupa://auth/confirmed y se autorizaron exactamente
cinco retornos de iOS normal/Staging y Android. SMTP personalizado está desactivado;
falta elegir proveedor/remitente sin desactivar confirmación. No se inventaron
contraseñas ni se enviaron correos a los usuarios. La función delete-account ya
está activa y rechaza solicitudes sin sesión o con JWT inválido (401); falta ensayo
autenticado. Registro/confirmación y prueba física siguen pendientes de SMTP.

## Acceso cUpa y Google (implementado, pendiente de prueba física)

Pantalla propia con la marca cUpa, «Hecha por compitas del café», «Sin cargos
extras», inicio/registro separados y acceso obligatorio. La gratuidad describe
cargos al usuario; no promete costo cero del backend. Rectángulo decorativo listo
para reemplazar por el PNG oficial de la mascota sin tapar los controles.

Política actual: AppShell no monta las pestañas sin identidad registrada/sesión.
Login raíz sin Cerrar ni Continuar sin cuenta; cierre de sesión/revocación
conocida regresa al acceso. Una sesión ya autenticada conserva uso local offline;
el primer acceso requiere internet. Auth sigue registrando usuarios/perfiles;
la biblioteca privada NO se sube a Supabase ni se borra al bloquear la entrada.

Botón Google conectado a ASWebAuthenticationSession y PKCE S256. Verificador
aleatorio sólo en memoria, nunca en URL ni UserDefaults; intento único de 10 minutos,
canje REST a Supabase y persistencia final en Keychain. No hay sesión por callback
sin intento local ni por tokens de fragmento. Cancelación/replay no autentican.
Si se cierra el proceso se reinicia el acceso, sin persistir secretos de OAuth.
Proyecto actual: `nice-column-512202-g6`, propiedad de la cuenta de cUpa,
sin activar facturación. Marca OAuth creada; soporte/contacto oficiales:
`cupabrewthers@gmail.com`, audiencia externa en pruebas. Proyecto anterior
`cupa-512202` de la cuenta personal intacto, SIN USAR para esta integración.
Cliente web «cUpa · Supabase Auth» creado por el propietario. Google activo,
verificado por `/auth/v1/settings` (`external.google=true`), confirmación de correo
conservada (`mailer_autoconfirm=false`). Secreto sólo en Supabase, no leído por
el agente ni enviado al código. Callback Google web:
`https://qndojncusqmhoxnokgwb.supabase.co/auth/v1/callback`.
Retorno exacto normal/Staging/Android mediante `/auth/confirmed`, reutilizando
la allowlist existente. No acepta tokens sin verificar ni modifica RLS/tablas.
Probar cancelación, retornos adulterados/repetidos, refresh, reinicio y ambos
dispositivos antes de beta pública. No subir biblioteca privada al login.

La captura entregada se usa exclusivamente como ícono: AppIcon 1024×1024 opaco,
imagen completa sin recortar al personaje ni redibujarlo. Conserva márgenes blancos
de la captura. El rectángulo de la futura mascota del login permanece separado.

Verificación 2026-10-19: build genérico iOS sin firma aprobado y verificador de
dominio `--google-oauth-only` aprobado (RFC7636, callbacks, expiración, cancelación,
replay y canje simulado con guardado de sesión). Android assembleDebug y 4 pruebas
GoogleOAuthFlowTest aprobadas. `/authorize` real devuelve 302 a accounts.google.com
con el cliente correcto y el callback exacto. Asesor de seguridad sin hallazgos.
No hubo login real ni consentimiento de un usuario; no se ha probado el secreto
del proveedor, el retorno físico en ambos teléfonos ni la homologación visual.

Antes de publicar con Google revisar App Store 4.8: ofrecer acceso equivalente
con privacidad, normalmente Sign in with Apple. El correo por sí solo no garantiza
cumplimiento. No activar un proveedor vacío ni deshabilitar confirmación de correo.

Referencias: https://supabase.com/docs/guides/auth/social-login/auth-google
y https://developer.apple.com/app-store/review/guidelines/#login-services

### Reingreso después de cerrar sesión

Reporte real de rechazo genérico al reingresar en iPhone. Auth consultado devuelve
callbacks 302 sin error del proveedor; no identifica la condición exacta del cliente.
Corrección defensiva compartida: aceptar sólo fragmentos vacíos/marcadores inocuos
(``, `_`, `sb`, `sb=`) y slash final canónico. Tokens en fragmento/query, duplicados,
host ajeno, intentos vencidos y replays siguen rechazados. Diagnóstico G01–G08
sin mostrar URL, códigos OAuth ni mensajes crudos de terceros. ASWebAuthenticationSession
ahora comprueba el ID de intento antes de completar/cancelar; una sesión vieja
no puede cancelar la nueva. Se agrega regresión simulada login → logout → login.
La cancelación de AccountView se liga al NavigationStack raíz, no a Group: los
modificadores de Group se propagan al contenido condicional y el paso de formulario
a Conectando no debe tratarse como abandonar la pantalla.
El propietario confirmó en iPhone que cerrar sesión y volver a entrar con Google
ya funciona. Esa confirmación real no sustituye la prueba pendiente en Android.
