# Cupa para iOS

Migración nativa en curso de Brew Studio Mobile a iPhone.

## Abrir

1. Abre `Cupa.xcodeproj` con Xcode.
2. En la parte superior, elige un simulador de iPhone.
3. Pulsa el botón triangular **Run**.

Esta etapa incluye navegación principal, diseño visual base, calculadora bidireccional portada desde Android, Laboratorio con fórmula sensorial exacta, altitud, hervor, °C/°F y experimentos persistentes, además del CRUD offline de cafés, molinos, equipos, recetas y técnicas con Core Data y borrado lógico. La preparación ejecuta técnicas reales con cronómetro guiado o manual y guarda sesiones históricas. Cata incluye rueda de sabor, perfil, NPS, historial editable, observaciones durante el enfriamiento, recuperación tras cierre y relación opcional con una preparación.

La pantalla de cuenta está preparada para Supabase Auth y almacena tokens en Keychain. La sincronización dispone de outbox persistente, reintentos y resolución de conflictos; la migración y RLS están en `supabase/migrations`. Mientras no se definan `SUPABASE_URL` y `SUPABASE_ANON_KEY`, la app indica que la cuenta aún no está conectada y todos los módulos locales siguen funcionando.

Gemini se invoca únicamente desde una Edge Function autenticada y la app conserva un fallback local. Las instrucciones de configuración, despliegue y Archive están en `DEPLOYMENT.md`.

Antes del primer envío remoto a Gemini, la app explica qué parámetros se compartirán con Google y solicita permiso revocable. El manifiesto `PrivacyInfo.xcprivacy` declara los datos sincronizados y el uso local de preferencias sin tracking.

Configuración permite tema del sistema, claro u oscuro y preferencias de unidades. Brew Hub ofrece perfil editable, privacidad y estadísticas derivadas exclusivamente de los datos reales guardados.

La comunidad permite publicar e importar recetas/técnicas con atribución, marcar contenido, bloquear autores y reportar publicaciones. Sin backend configurado se muestra un estado vacío real y los datos privados permanecen locales.

El proyecto incluye el AppIcon de la referencia Android y semántica VoiceOver para controles de icono, áreas táctiles mínimas e identificadores de automatización para los flujos principales.

Al iniciar sesión, la sincronización reclama los datos creados offline, sube la outbox y descarga cambios incrementales de todos los agregados para restaurarlos en otro dispositivo. La validación contra el proyecto Supabase real requiere las dos variables públicas descritas en `BLOCKERS.md`.

El target `CupaTests` contiene pruebas XCTest. Si CoreSimulator no inicia, `Tools/LabGoldenVerifier.swift` valida cuatro casos dorados, restauración de estado y persistencia Core Data desde macOS; el comando está en `TEST_PLAN.md`.

El target `CupaUITests` contiene recorridos de interfaz y lanza una base Core Data en memoria mediante el argumento `-ui-testing`, sin alterar ni depender de datos personales del simulador.

Supabase, sincronización multiusuario, Gemini mediante backend y los demás CRUD siguen el estado documentado en `PARITY_MATRIX.md` y `BLOCKERS.md`.
