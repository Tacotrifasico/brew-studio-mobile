# Cupa Android · biblioteca local, cuentas y comunidad

Alcance vigente del propietario (19 octubre 2026): **NO sincronizar el Almacén**.
Granos/ajustes por método, equipo, recetas/técnicas privadas, tazas, catas y
laboratorio viven en Room. Sólo la acción Compartir envía un snapshot explícito.
Ingresar, guardar o importar no crea una biblioteca remota.

El backend compartido está en `qndojncusqmhoxnokgwb`. La documentación completa
y las migraciones aplicadas están en la rama `ios-production-migration`:
`Backend/README.md`, `Backend/supabase/migrations`, `AXCIS_BACKEND_HANDOFF.md`.
No reaplicar el SQL inicial Android ni la cola histórica iOS de la raíz.

## Implementado

- SyncRepository no hace push/pull de bibliotecas. La API privada está revocada.
- Publicar acepta UUID local, sin sincronizar primero una receta/técnica privada.
- Snapshot completo, validado; importar/fork guarda padre+hijos de forma local
  transaccional, con UUID nuevos, autoría y publicación de origen.
- Sin llamadas a las RPC de copia remota. No implementarlas con el alcance actual.
- Envíos directos por @alias; el servidor valida acceso y conserva al creador al
  republicar una variante mediante `source_share_id`.
- Servidor deriva nombre/alias y autor real desde el perfil. Los clientes no pueden
  falsificar un autor ajeno ni editar el contador de likes.
- Likes idempotentes y contador del servidor; estado propio consultado para el lote.
- Consultas de comunidad acotadas a 30 registros/carga; sin Realtime ni buckets.
- Sesión: access/refresh/expiración cifrados con Android Keystore. Sin fallback
  persistente en texto plano; si Keystore falla, sólo memoria hasta cerrar proceso.
- Migración de sesión antigua exige ingresar de nuevo; NO borra el inventario.
- Una respuesta de registro sin tokens pide confirmar correo y no inicia sesión.
- Perfil canónico sin columnas email/role inventadas; control Perfil privado.
- No enviar cuerpos Auth a logs ni respaldar credenciales por backup/transferencia.
  Se conservan los respaldos del SO del resto de datos, independientes de Supabase.
- Almacén no muestra avisos engañosos de subida pendiente; datos locales son normales.
- IA deshabilitada. Nunca agregar claves de IA, service_role ni secretos OAuth al
  cliente. Un identificador público de cliente OAuth no es un secreto.

## Pruebas y operación

El baseline pasó 31 pruebas SQL y el alcance local-first otras 33, además del
ensayo real con roles del servidor (fixtures con ROLLBACK, sin emails).
El asesor de seguridad no reporta hallazgos. No confundir con cuentas/JWT reales.

Panel revisado con el propietario: redirects exactos ya guardados, sin localhost.
Delete-account está desplegada; dos pruebas HTTP rechazaron correctamente acceso
sin JWT/con JWT inválido. Pendiente externo: elegir proveedor/remitente SMTP y
probar correos y eliminación autenticada antes de beta pública. El propietario debe elegir
contraseñas en las apps y confirmar dos correos propios para probar
Android ↔ iPhone: muro, directo, importar, variante, likes, atribución,
offline/reinicio/cambio de cuenta. No declarar validación visual sin verla.

No presupuestar costo cero: medir transferencia/publicaciones y configurar
alertas/límites del plan; esta fase no contrata otro proyecto, plan o servicio.

## Acceso cUpa y Google (implementado, pendiente de prueba física)

La pantalla de acceso muestra cUpa, «Hecha por compitas del café» y «Sin cargos
extras». Este último mensaje describe cargos al usuario, NO garantiza costo cero
de infraestructura. El rectángulo decorativo espera el PNG oficial de la mascota.
Actualización de política: login OBLIGATORIO en la raíz de la app. Sin sesión
registrada no se monta la navegación principal; no hay acceso invitado ni demo.
Cerrar sesión/revocación conocida devuelve al login. Una sesión previamente
autenticada permite herramientas locales offline; el primer acceso requiere red.
Auth ya registra usuarios/perfiles; esto NO añade sincronización de la biblioteca.

Botón Google conectado a navegador externo y PKCE S256. Verificador aleatorio
cifrado con Keystore; sólo se consume una vez y vence en 10 minutos. El retorno
exacto `${applicationId}://auth/confirmed` sólo acepta código; no acepta tokens
del fragmento ni callbacks sin intento local. Cancelar/entrar por correo invalida
el intento; los tokens de sesión siguen en el almacenamiento seguro existente.
Proyecto actual: `nice-column-512202-g6`, propiedad de la cuenta de cUpa,
sin activar facturación. Marca OAuth creada; soporte/contacto oficiales:
`cupabrewthers@gmail.com`, audiencia externa en pruebas. El proyecto anterior
`cupa-512202` de la cuenta personal se conserva SIN USAR, no fue eliminado.
Cliente web «cUpa · Supabase Auth» creado por el propietario. Google activo,
verificado por `/auth/v1/settings` (`external.google=true`), confirmación de correo
conservada (`mailer_autoconfirm=false`). Secreto sólo en Supabase, no leído por
el agente ni copiado al cliente. Para flujo web
el callback de Google es
`https://qndojncusqmhoxnokgwb.supabase.co/auth/v1/callback`.
El retorno reutiliza la allowlist existente; no agrega comodines ni cambia
permisos del backend. Probar cancelación, retorno manipulado/repetido, reinicio, refresh y cambio de
cuenta en ambos teléfonos. No subir biblioteca local al iniciar sesión con Google.
Revisar además App Store 4.8 y ofrecer en iOS un acceso equivalente que cumpla
privacidad (normalmente Sign in with Apple); no afirmar que correo basta.

La captura entregada es exclusivamente el ícono de instalación: master cuadrado
1024 sin alpha, tamaños legacy y adaptive-icon con zona segura. Se conserva la
imagen completa y su margen blanco; no se regeneró ni recortó el personaje.
El login NO usa ese ícono como mascota; conserva el rectángulo de reserva.

Verificación 2026-10-19: assembleDebug aprobado y 4 pruebas GoogleOAuthFlowTest
aprobadas (RFC7636, aleatoriedad, URL sin verificador, callbacks inválidos).
Pruebas de dominio iOS también aprobadas; build genérico de iOS sin firma aprobado.
Sondeo real `/authorize`: HTTP 302 a accounts.google.com con ID de cliente correcto
y callback exacto. Asesor de seguridad: sin hallazgos. Esto NO prueba el secreto
de Google ni el login real: faltan consentimiento y retorno en ambos teléfonos,
validación visual real, cancelación/reinicio y prueba social con dos usuarios.

Referencias: https://supabase.com/docs/guides/auth/social-login/auth-google
y https://developer.apple.com/app-store/review/guidelines/#login-services

### Reingreso después de cerrar sesión

Reporte real: iPhone mostró el mensaje genérico al reingresar con Google. Los logs
consultados de Auth muestran callbacks 302 sin error del proveedor; no prueban
la causa exacta del rechazo local. Se homologó la compatibilidad de retorno con
fragmento vacío/marcadores inocuos (`#`, `#_`, `#sb=`) y slash final; nunca se aceptan
tokens en URL ni se omite PKCE. Mensajes G01–G08 distinguen el motivo sin exponer
códigos, URLs o descripciones crudas del proveedor. iOS aísla cancelaciones tardías
del navegador por intento. Regresión simulada login → logout → login, además de
replay/cancelación. Sigue pendiente repetir el caso real en ambos teléfonos.
