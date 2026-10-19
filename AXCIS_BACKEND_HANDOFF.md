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
