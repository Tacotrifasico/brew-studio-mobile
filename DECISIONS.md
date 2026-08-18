# Decisiones técnicas

## D-001 — Aplicación nativa SwiftUI

Se mantiene el proyecto iOS nativo en SwiftUI. Razón: ya existe una base compilable, permite comportamiento iOS nativo y evita condicionar la entrega a una refactorización completa del proyecto Android. Las fórmulas se portan literalmente y se validan con pruebas de paridad.

## D-002 — Core Data para caché offline

Se usa Core Data como persistencia local. Las entidades mantienen UUID, propietario, fechas, versión, borrado lógico y estado de sincronización para poder mapearse a Supabase. La primera implementación usó SwiftData, pero Xcode 26.6 instalado no puede ejecutar `SwiftDataMacros` porque su firma local no es confiable (`CSSMERR_TP_NOT_TRUSTED`). Core Data conserva almacenamiento nativo y evita que una avería de la instalación bloquee la aplicación.

## D-003 — Supabase como verdad multiusuario

Core Data es caché offline; Supabase será la fuente remota. Conflicto por defecto: mayor `updated_at` validado, salvo reglas específicas documentadas posteriormente.

## D-004 — Sin datos demostrativos en producción

Se eliminan cafés, estadísticas y contenido social inventados. Los estados sin datos usan vistas vacías explícitas.

## D-005 — Gemini fuera del cliente

Gemini se invocará desde una Supabase Edge Function autenticada. Ninguna API key privada se incluirá en el bundle.

## D-006 — Fórmulas exactas

Calculadora y Laboratorio reproducen operaciones, límites, truncamiento y redondeo de Android. No se reemplazan por aproximaciones de Swift.

## D-007 — Estado interno del Laboratorio en Celsius

La temperatura se almacena y calcula siempre en Celsius, como Android. Fahrenheit es sólo una presentación reversible. Altitud, ciudad, unidad y variables activas se guardan en `UserDefaults`; los experimentos confirmados son entidades Core Data sincronizables.

## D-008 — Verificador dorado ejecutable

Además de XCTest, `Tools/LabGoldenVerifier.swift` permite ejecutar el motor puro en macOS cuando CoreSimulator no está disponible. Sus valores esperados provienen literalmente de `calculateLabProfile` en el commit Android auditado y no se recalculan desde la implementación iOS.

## D-009 — Agregados separados para receta y técnica

`Recipe` posee ingredientes e instrucciones; `Technique` posee pasos ejecutables y sólo referencia opcionalmente una receta. Los hijos conservan UUID propios y metadatos de sincronización. La edición reconcilia por UUID, el reordenamiento actualiza índices y los elementos retirados se borran lógicamente para que una sincronización futura pueda propagarlos.

## D-010 — Preparación recuperable con snapshots históricos

El cronómetro conserva en `UserDefaults` el último instante observado y el estado activo. Al volver del fondo suma el tiempo transcurrido y avanza de forma determinista en modos guiado o automático; el modo manual sólo cambia de paso por acción del usuario. Al finalizar crea un `BrewSession` en Core Data con UUID de las entidades relacionadas y snapshots de técnica, método, café, molino y pasos, para que el historial no se corrompa si el inventario se edita o elimina después.

## D-011 — Cata, observaciones y taza como agregados separados

Una `Tasting` puede guardarse sin preparación y enlazarse opcionalmente a un `BrewSession`. Cada lectura durante el enfriamiento es una `TastingObservation` con UUID, etapa, tiempo y perfil propios. `CupSession` enlaza ambos agregados y conserva snapshots mínimos; no convierte receta, técnica o cata en la misma entidad. Las etapas térmicas preservan los límites de Android: caliente antes de 4 min, pico antes de 10 min, descenso antes de 16 min y agotada desde entonces.

## D-012 — Tokens en Keychain y outbox persistente

La sesión Supabase se guarda en Keychain con acceso posterior al primer desbloqueo y nunca en `UserDefaults`. La anon key es configuración pública por ambiente; `service_role` está prohibida en el cliente. Las mutaciones offline se compactan por entidad en una outbox Core Data, reintentan con espera exponencial y resuelven conflictos primero por `updated_at` y después por versión. Una diferencia de propietario nunca se combina y se trata como violación de aislamiento.

## D-013 — Gemini opcional y autenticado

El cliente envía sólo entradas numéricas y de método a una Edge Function con JWT de usuario. La función valida, limita a cinco solicitudes por minuto, aplica timeout y usa `GEMINI_API_KEY` únicamente en servidor. Gemini interpreta resultados ya calculados; nunca produce el índice ni las puntuaciones. Cualquier ausencia de sesión, red, cuota, timeout o salida inválida activa una sugerencia local determinista y claramente etiquetada.

## D-014 — Tema adaptativo y estadísticas derivadas

Los colores semánticos usan proveedores dinámicos de UIKit para responder a claro/oscuro sin duplicar vistas. La elección sistema/claro/oscuro y la unidad Celsius/Fahrenheit persisten en `UserDefaults`. Brew Hub cuenta directamente entidades Core Data no eliminadas; no guarda contadores ni inserta actividad demostrativa. El perfil es propiedad del UUID autenticado y se conserva offline con estado de sincronización.

## D-015 — Publicaciones por snapshot y moderación en profundidad

Compartir crea un snapshot explícito de receta o técnica; importar genera UUID nuevos y conserva `originalEntityId` y `copyMode=IMPORT`. El feed sólo consulta contenido real `PUBLIC/ACTIVE`. RLS excluye relaciones bloqueadas y separa mensajes directos. Likes y guardados sólo son visibles/editables por su usuario; reportes son visibles al denunciante y quedan listos para revisión administrativa. Correos y otros identificadores sensibles nunca forman parte del payload público. La publicación valida identidad, longitudes y frases objetables en iOS para respuesta inmediata y repite la regla mediante trigger SQL sobre todo el snapshot, evitando que REST directo eluda el filtro.

## D-016 — Sincronización incremental por descriptor

Cada tipo Core Data tiene un descriptor explícito de tabla, columnas y conversiones; no se serializan propiedades internas por reflexión indiscriminada. Al autenticarse, los registros locales sin propietario se reclaman para ese usuario, se compactan en outbox y se suben antes de descargar cambios incrementales. El pull respeta orden de padres/hijos, propietario, `updated_at`, versión y borrado lógico. El checkpoint sólo avanza cuando todas las tablas terminan, por lo que un fallo no pierde cambios.

## D-017 — Privacidad declarada y consentimiento explícito para IA

`PrivacyInfo.xcprivacy` declara los datos sincronizados, ausencia de tracking y el uso local de `UserDefaults` con la razón aprobada `CA92.1`. Gemini sólo recibe parámetros de preparación y perfil sensorial después de una autorización explícita y revocable; sin permiso se fuerza el motor local. La URL legal se inyecta por ambiente con `PRIVACY_POLICY_URL` y nunca se fija una política ficticia en el código.

## D-018 — Ícono derivado de la referencia Android

El AppIcon usa el recurso gráfico vigente de Android convertido a PNG RGB de 1024 × 1024, sin inventar una identidad visual nueva. Se conserva en un catálogo estándar de Xcode para que App Store genere sus variantes.

## D-019 — Referencias estables del Laboratorio y snapshots de receta

Laboratorio conserva UUID opcionales de receta, técnica, método/equipo, café y molino, además de sus parámetros editables. Al transferir a Preparación se preservan esos UUID y el nombre de la técnica; al finalizar, `BrewSession` guarda también `methodId` y `recipeNameSnapshot`. Las referencias sirven para sincronización y navegación, mientras los snapshots conservan el significado histórico tras una edición o borrado lógico. Los atributos nuevos son opcionales o tienen valores por defecto y Core Data mantiene migración ligera automática.

## D-020 — Preparación rápida equivalente y entradas válidas persistentes

Calculadora persiste únicamente el último estado numérico válido; el texto incompleto mientras el usuario edita nunca reemplaza ese snapshot. `onActionPrepare` usa los títulos, duraciones, instrucciones y distribución de agua de `generateQuickSteps` en Android, con avance guiado y temperatura de 93 °C. Para volúmenes menores que el bloom de 40/50 ml se limita primero el bloom y se reparte el remanente, evitando cantidades negativas sin cambiar los resultados de los casos normales de la referencia.

## D-021 — Finalización recuperable y guardado idempotente de preparación

Los modos `GUIDED` y `AUTOMATED` terminan al alcanzar la suma exacta de sus pasos y acotan el tiempo si la app vuelve del fondo después del límite. Una sesión completada pero todavía no guardada se restaura para no perder trabajo; después de crear `BrewSession`, `savedAt` oculta la acción repetida. Reiniciar una sesión ya guardada asigna un UUID nuevo, evitando colisiones o duplicados históricos.

## D-022 — Historial de tazas como snapshot autónomo

`CupSession` replica el conjunto histórico de Android y se muestra localmente en Almacén, sin exigir cuenta. Al guardar una cata copia referencias, parámetros ejecutados, nombres, vida de taza, calificación, NPS, comentario y fecha desde `BrewSession` y `Tasting`. Borrar una cata o su taza marca también la otra y sus observaciones como borradas, evitando filas históricas huérfanas; borrar inventario no destruye los snapshots. El selector de Almacén usa botones desplazables porque seis categorías no caben de forma accesible en un control segmentado de iPhone.

## D-023 — Frescura del café derivada, no persistida

El estado de frescura se calcula al mostrar o editar el café a partir de las fechas persistidas de tueste y apertura; no se guarda una etiqueta que pueda quedar obsoleta. Se preservan los cortes, interpolación continua, textos y alerta de bolsa abierta de Android. La diferencia de días replica su normalización a medianoche y truncamiento de milisegundos, incluso alrededor de cambios de horario local.

## D-024 — Configuración sin controles aparentes

Se elimina el interruptor de unidades métricas porque sólo persistía un booleano y no alteraba ninguna medida; Android usa gramos y mililitros sin modo imperial. Celsius/Fahrenheit permanece porque sí convierte la presentación del Laboratorio. Los formularios de café, molino y equipo conservan el editor abierto y muestran el error real cuando Core Data no puede guardar, en lugar de descartarlo silenciosamente.
