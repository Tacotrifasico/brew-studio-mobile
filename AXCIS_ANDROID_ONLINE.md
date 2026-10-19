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
- IA deshabilitada. Nunca agregar Google keys, service_role ni secrets al cliente.

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
