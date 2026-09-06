# Objetivo del proyecto

Entregar Brew Studio (Cupa) para iOS como aplicación nativa estable, homologada funcionalmente con la referencia Android/Google AI Studio y preparada técnicamente para TestFlight y App Store.

La homologación exige:

1. Datos equivalentes, persistencia offline, propiedad por usuario y sincronización segura.
2. Fórmulas, reglas, CRUD, temporizadores y flujos completos equivalentes.
3. Capacidades y jerarquía visual equivalentes, adaptadas a patrones nativos de iOS.

El proyecto no se considera terminado porque compile o muestre pantallas. La definición de terminado está en `RELEASE_CHECKLIST.md` y requiere paridad validada, pruebas críticas aprobadas, build Release y Archive.

## Fuentes de verdad

1. Rama `main` de `https://github.com/Tacotrifasico/brew-studio-mobile`.
2. Implementación Android vigente y su esquema Supabase.
3. Proyecto iOS de este directorio.
4. Documentos de decisiones y paridad.

Última referencia Android auditada: commit `392fd2bbb3c4b906d30af1799ca15612eafa34bb`, integrado y confirmado contra `origin/main` el 5 de septiembre de 2026. Incluye la continuidad Calculadora → Preparar y la selección persistente del favorito.

Último avance iOS: el envío directo recibe un alias público y lo resuelve mediante una RPC autenticada, sin pedir ni mostrar UUID de cuenta. La migración conserva el contrato Android y fue ejecutada en las dos rutas de actualización sobre PostgreSQL efímero.

Cuenta contempla la configuración productiva habitual de Supabase: si el alta no entrega sesión porque exige verificar correo, conserva el estado desconectado, explica el siguiente paso y permite reenviar la confirmación. Recuperar contraseña confirma la solicitud sin revelar si el correo está registrado, abre de nuevo Cupa mediante un enlace propio por ambiente y permite guardar la contraseña nueva con el token temporal sin crear una sesión persistente.

La sincronización incremental conserva una frontera tomada del reloj HTTP de Supabase al comenzar el recorrido y retrocede cinco minutos de forma deliberada. Así, una edición remota concurrente no queda detrás de un checkpoint creado al final; si el servidor omite la fecha se aplica el mismo solapamiento al inicio local.
