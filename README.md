# Cupa para iOS

Migración nativa en curso de Brew Studio Mobile a iPhone.

## Abrir

1. Abre `Cupa.xcodeproj` con Xcode.
2. En la parte superior, elige un simulador de iPhone.
3. Pulsa el botón triangular **Run**.

Esta etapa incluye navegación principal, diseño visual base, calculadora bidireccional portada desde Android, Laboratorio con fórmula sensorial exacta, altitud, hervor, °C/°F y experimentos persistentes, además del CRUD offline de cafés, molinos, equipos, recetas y técnicas con Core Data y borrado lógico. La preparación ejecuta técnicas reales con cronómetro guiado o manual y guarda sesiones históricas. Cata incluye rueda de sabor, perfil, NPS, historial editable, observaciones durante el enfriamiento, recuperación tras cierre y relación opcional con una preparación.

La pantalla de cuenta está preparada para Supabase Auth y almacena tokens en Keychain. La sincronización dispone de outbox persistente, reintentos y resolución de conflictos; la migración y RLS están en `supabase/migrations`. Mientras no se definan `SUPABASE_URL` y `SUPABASE_ANON_KEY`, la app indica que la cuenta aún no está conectada y todos los módulos locales siguen funcionando.

Gemini se invoca únicamente desde una Edge Function autenticada y la app conserva un fallback local. Las instrucciones de configuración, despliegue y Archive están en `DEPLOYMENT.md`. El esquema `Cupa` usa Development al ejecutar y Production al archivar; `Cupa-Staging` utiliza un backend y bundle separados para pruebas previas a producción.

Antes del primer envío remoto a Gemini, la app explica qué parámetros se compartirán con Google y solicita permiso revocable. El manifiesto `PrivacyInfo.xcprivacy` declara los datos sincronizados y el uso local de preferencias sin tracking.

Configuración permite tema del sistema, claro u oscuro y preferencias de unidades. Brew Hub ofrece perfil editable con avatar de iniciales/color, validación, privacidad efectiva y estadísticas derivadas exclusivamente de los datos reales guardados.

La comunidad permite publicar recetas/técnicas en el muro o enviarlas a un UUID concreto, recibirlas en un buzón con estado de lectura, registrar copias, crear variantes con atribución, guardar, marcar Me gusta, bloquear/desbloquear autores y reportar publicaciones con motivo y detalles. Un perfil privado sólo puede hacer envíos directos, con defensa adicional en Supabase. El historial muestra actividad social real junto con preparaciones y catas locales. Sin backend configurado se muestra un estado vacío real y los datos privados permanecen locales.

El proyecto incluye el AppIcon de la referencia Android y semántica VoiceOver para controles de icono, áreas táctiles mínimas e identificadores de automatización para los flujos principales.

Al iniciar sesión, la sincronización reclama los datos creados offline, sube la outbox y descarga cambios incrementales de todos los agregados para restaurarlos en otro dispositivo. La sesión sobrevive a cortes de internet sin reutilizar tokens vencidos y la app reintenta automáticamente al recuperar red o volver al primer plano. La validación contra el proyecto Supabase real requiere las dos variables públicas descritas en `BLOCKERS.md`.

El contrato remoto usa las tablas oficiales de Android (`beans`, `shares` y `user_id` en las entidades compartidas), de modo que Android y iOS no crean inventarios o comunidades paralelos. Las migraciones conservan las columnas históricas y traducen los campos necesarios; deben probarse sobre un clon Staging antes de aplicarse a producción.

Los datos locales también están separados por cuenta: cerrar sesión oculta inmediatamente el contenido del propietario anterior y una cuenta distinta usa sus propias entidades, cola de sincronización, Calculadora, Laboratorio, Preparación y Cata. El trabajo creado sin cuenta permanece como invitado y puede ser reclamado por el primer usuario que sincronice.

El target `CupaTests` contiene pruebas XCTest. Si CoreSimulator no inicia, `Tools/LabGoldenVerifier.swift` valida cuatro casos dorados, restauración de estado y persistencia Core Data desde macOS; el comando está en `TEST_PLAN.md`.

El target `CupaUITests` contiene recorridos de interfaz y lanza una base Core Data en memoria mediante el argumento `-ui-testing`, sin alterar ni depender de datos personales del simulador.

Supabase, sincronización multiusuario, Gemini mediante backend y los demás CRUD siguen el estado documentado en `PARITY_MATRIX.md` y `BLOCKERS.md`.
