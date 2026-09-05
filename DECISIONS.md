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

## D-025 — Reapertura SQLite como prueba de persistencia

El controlador admite inyectar una URL de almacén sólo para aislar pruebas sin tocar los datos reales. El verificador crea un SQLite temporal, guarda un café con UUID, fecha y existencias, desmonta completamente el almacén, abre un contenedor nuevo y comprueba los mismos valores. El historial persistente puede desactivarse en esa prueba aislada para desmontar el store sin notificaciones del sistema; producción lo conserva activado por defecto.

## D-026 — Historial de uso del café derivado por UUID

El detalle de cada café consulta `BrewSession` y `CupSession` activas por su `beanId`; no persiste una lista duplicada ni contadores que puedan desincronizarse. La pantalla muestra preparaciones, dosis, agua, proporción, fechas, tazas, valoración, etapa térmica y comentario. El borrado lógico del café lo retira del inventario, pero conserva las sesiones y sus snapshots para que el historial siga siendo legible y sincronizable.

## D-027 — Estado del lote derivado y acciones equivalentes

Android representa cerrado, abierto y terminado; en iOS se derivan de los datos ya sincronizados: sin fecha de apertura, con fecha de apertura y existencias en cero, respectivamente. “Abrir bolsa hoy” guarda la fecha actual y “Marcar como terminado” lleva las existencias a cero, evitando un segundo campo de estado que pueda contradecirlos. Preparar conserva el UUID en el estado recuperable de preparación; Laboratorio conserva UUID, frescura calculada, proceso y notas. La referencia Android auditada no contiene fotografía en `Bean`, su formulario ni su detalle, por lo que no se agrega un selector aparente sin función de origen.

## D-028 — Perfil del molino y ajuste de la técnica separados

El formulario Android vigente del molino expone marca, modelo, rango operativo y calibración; no ofrece edición de ajustes por método aunque el DTO contemple esa colección futura. iOS conserva esos datos como nombre/marca/modelo, unidad, mínimo, máximo y calibración. El valor interno y su descripción legible pertenecen a cada técnica y se congelan en la sesión ejecutada, porque “22 clicks” puede variar entre métodos y molinos y no es una propiedad única del equipo.

## D-029 — Importación de recetas local y revisable

El importador replica el contrato heurístico de Android en el dispositivo: reconoce encabezados, cantidades, unidades, pasos, perfil, categoría y método sin enviar el texto a Gemini ni exigir conexión. El resultado abre el editor normal antes de persistirse, de modo que una inferencia imperfecta nunca se guarda sin revisión del usuario. La búsqueda incluye también intención, ingredientes e instrucciones, y favoritas es un filtro real sobre el mismo agregado Core Data.

## D-030 — Detalle de receta antes de mutar

Tocar una receta abre una vista de lectura con intención, método, etiquetas, ingredientes y pasos ordenados, como en Android. Editar espera a que el detalle termine de cerrarse antes de presentar el formulario, evitando dos hojas simultáneas. El borrado exige confirmación y explica que los snapshots históricos no se eliminan; favorita, duplicación y edición reutilizan el mismo repositorio del listado.

## D-031 — Métodos del inventario reutilizados por la calculadora

Los equipos activos de tipo `BREWER_METHOD` forman el catálogo personalizado de métodos, como en Android. `isFavorite` significa “fijado en la calculadora” para ese tipo y se edita tanto desde Almacén como desde el gestor de métodos; los cuatro métodos base fijados inicialmente viven en `UserDefaults`. No se crea una entidad duplicada. Los métodos base conservan sus siete ratios exactos y un equipo personalizado usa 1:15, el mismo fallback del `onMethodSelected` Android. Su UUID se conserva en el estado, favoritos y transferencias a Laboratorio y Preparación.

## D-032 — Detalle de técnica como frontera antes de ejecutar o mutar

La biblioteca abre primero un detalle legible con parámetros y secuencia completa, en vez de entrar directamente al formulario. Desde ahí se puede ejecutar la técnica, editarla o eliminarla con confirmación. Ejecutar carga el agregado persistido y sus pasos ordenados en el mismo `PreparationModel` recuperable usado por la pestaña Preparar; no crea una copia provisional. El borrado lógico mantiene intactos los snapshots de sesiones históricas.

## D-033 — Historial de cata completo y guardado explícitamente nuevo

El historial no se limita a las últimas ocho filas: cada cata abre un detalle con preparación vinculada, perfil, atributos y evolución térmica completa. Editar reutiliza su UUID y reconcilia observaciones y taza asociada; eliminar exige confirmación y conserva la preparación original. Después de guardar, la misma cata queda bloqueada para un segundo guardado ambiguo. “Nueva” crea otro UUID y restablece el progreso, evitando sobrescribir una evaluación previa bajo una etiqueta de alta nueva.

## D-034 — Detalle de inventario antes de editar o eliminar

Molinos y equipos abren una ficha de lectura con todos sus datos persistidos antes de cualquier mutación. Editar conserva el UUID y marca una actualización sincronizable; eliminar exige confirmación y realiza borrado lógico. Las preparaciones previas mantienen los UUID y snapshots de molino y método, de modo que retirar inventario no vuelve ilegible el historial. La capacidad de un equipo sólo acepta enteros positivos y el rango del molino se mantiene ordenado.

## D-035 — Cierre inmutable y reinicio confirmado de cata

Una cata completada no admite reinicio ni nuevas observaciones: para otra evaluación se debe usar “Nueva”, que asigna otro UUID. Esto impide que reiniciar después de guardar sobrescriba silenciosamente el historial. Mientras la cata está activa, sus observaciones térmicas son visibles y removibles; reiniciar tiempo y observaciones exige confirmación cuando ya existe progreso. Guardar detiene primero el reloj para persistir un tiempo coherente y deja el estado pausado si Core Data falla.

## D-036 — Validación estricta y recuperación no destructiva

El café se valida antes de tocar Core Data: altitud, cantidades finitas, existencias no mayores a la cantidad inicial y fechas coherentes. Los borrados y reinicios con información relevante se realizan desde un detalle o diálogo que explica su alcance; se eliminan los atajos de deslizar que omitían esa confirmación. Si el SQLite local no puede abrirse, la app conserva el archivo original, inicia un almacén temporal en memoria y muestra una advertencia persistente en vez de terminar con `fatalError`; así permite recuperar la interfaz sin ocultar que esa sesión no persistirá cambios.

## D-037 — Una sola preferencia térmica y selección geográfica exacta

Celsius/Fahrenheit usa `settings.temperature` como única preferencia entre Configuración y Laboratorio. El estado anterior del Laboratorio se migra a esa clave cuando todavía no existe y después siempre respeta la elección global, evitando volver silenciosamente a Celsius tras reiniciar. La temperatura de cálculo y de persistencia permanece en Celsius; Fahrenheit sólo convierte y redondea la presentación, igual que Android. Una ciudad se considera seleccionada únicamente cuando coinciden altitud e identidad, para que una calibración manual a la misma elevación no aparente ser una ciudad predefinida.

## D-038 — Navegación estable y color accesible por tema

Las cinco secciones conservan el orden Android y comparten un único modelo de selección para que cambiar de pestaña no reconstruya el estado de trabajo. La paleta define un color de texto sobre acentos para cada apariencia en vez de asumir blanco, y todos los pares usados como texto normal alcanzan contraste WCAG AA de 4.5:1 en claro y oscuro. Las métricas críticas usan estilos relativos de Dynamic Type; las filas ajustables pueden cambiar de distribución antes de truncar y mantienen etiquetas y valores accesibles para VoiceOver.

## D-039 — Ambientes compilables y configuración dentro del bundle

Un archivo `.xcconfig` sin configuración de Xcode asociada no constituye un ambiente real. El proyecto define Development, Staging y Production; Staging dispone de esquema compartido, nombre visible y bundle ID propios para convivir con producción. Se usa un `Info.plist` explícito porque la generación automática no estaba incorporando las claves personalizadas: el binario ahora recibe `APP_ENVIRONMENT`, Supabase y las URLs legales. Las URLs remotas de Supabase exigen HTTPS y los valores vacíos se normalizan a ausencia; HTTP sólo se permite para desarrollo local en localhost.

## D-040 — La conectividad transitoria no invalida la sesión

La identidad recuperada del Keychain permanece disponible cuando falla internet, aunque un access token vencido no se reutiliza para llamadas remotas. Un rechazo definitivo del refresh (`400/401`) limpia las credenciales y solicita un nuevo acceso; un `401` inesperado en una operación autenticada fuerza una sola renovación y reintento. `NWPathMonitor` dispara recuperación y sincronización al volver la conexión, además del intento al abrir o reactivar la app. La outbox conserva operaciones fallidas con su backoff y la interfaz informa que los datos locales siguen seguros.

## D-041 — Buzón directo y actividad social separados del contenido

La referencia Android distingue el muro público, transferencias directas, copias, variantes y actividad. iOS conserva `brew_shares` como snapshot fuente y crea `inbox_items` sólo como estado de entrega/lectura, generado por trigger para publicaciones `DIRECT`; el destinatario únicamente puede leer sus filas y actualizar `read_at`. `activity_log` registra acciones del propietario sin convertirse en estadística inventada. Registrar copia crea UUID nuevos con `copyMode=IMPORT`; crear variante hace lo mismo con `copyMode=FORK`. Fallar al escribir la actividad no convierte una publicación ya aceptada en fracaso ni induce un envío duplicado.

## D-042 — Privacidad de perfil y moderación aplicadas en dos capas

El avatar conserva la decisión Android de iniciales y color, sin introducir una carga de fotografías inexistente en la referencia. Nombre, alias, biografía, color y métodos favoritos se normalizan y validan antes de tocar Core Data, y las mismas cotas se aplican en Supabase. Un perfil privado sólo puede compartir por buzón: la interfaz elimina la opción pública y un trigger SQL rechaza cualquier intento de eludirla por REST. Los reportes exigen una categoría, admiten contexto limitado y quedan listos para revisión humana; bloquear requiere confirmación y la lista de bloqueados permite revertir la decisión. Las URLs HTTPS de normas, soporte/moderación y privacidad siguen siendo configuración legal por ambiente, no texto ficticio dentro del binario.

## D-043 — El ámbito local cambia con la identidad autenticada

Cada consulta de interfaz y repositorio limita Core Data al propietario activo, admite datos todavía sin propietario para su adopción inicial y excluye borrados lógicos. Las altas reciben inmediatamente el UUID activo; la outbox, el merge y el checkpoint se separan por propietario. Al cerrar sesión la identidad local se oculta antes de terminar la llamada remota y al cambiar de cuenta se reconstruyen las consultas. Calculadora, Laboratorio, Preparación, Cata, favoritos y trabajo recuperable usan claves de `UserDefaults` por propietario; el valor legado sin ámbito se migra una sola vez. Así una segunda cuenta no ve ni envía el trabajo privado de la primera, mientras el modo invitado continúa disponible.

## D-044 — Un solo contrato Supabase compartido con Android

La migración inicial iOS creó nombres paralelos (`coffee_beans`, `brew_shares`, `owner_id`) que no eran el contrato oficial de la referencia Android (`beans`, `shares`, `user_id`). Mantener ambos como fuentes editables habría dividido cuentas, inventario y comunidad. iOS sincroniza ahora las entidades compartidas directamente contra las tablas Android y usa sus nombres de propiedad; los agregados exclusivos de iOS conservan `owner_id`. `coffee_beans` permanece sólo como espejo relacional interno porque las sesiones iOS existentes ya lo referencian, nunca como segunda fuente de inventario. Los triggers detectan qué representación cambió y traducen campos legados/canónicos en ambos sentidos. Las publicaciones usan `shares`, visibilidad minúscula y snapshot plano compatible con Android; el decodificador iOS acepta también snapshots tipados anteriores para no perder publicaciones creadas durante la migración. Esta decisión reemplaza únicamente la afirmación de D-041 de que `brew_shares` sería la fuente social permanente; buzón, actividad, copias y atribución de D-041 permanecen vigentes.

## D-045 — Una sola calculadora compartida entre Taller y Preparar

Android presenta la calculadora barista completa en Inicio; iOS sólo mostraba un acceso que obligaba a cambiar de pestaña. Se extrae un componente SwiftUI único que observa el mismo `CalculatorModel` en Taller y Preparar, incluidos presets, métodos del inventario, fijados y favoritos. Así cualquier edición aparece inmediatamente en ambos lugares y las transferencias a Laboratorio o Preparación conservan los mismos identificadores y valores, sin duplicar lógica ni persistencia. El resumen del taller usa únicamente conteos y registros recientes de Core Data filtrados por el propietario activo; cuando no hay datos no inventa una receta o estadística de respaldo.

## D-046 — Atribución social transitiva y estados honestos del Hub

Una receta o técnica importada conserva autor original, entidad raíz, publicación fuente y modo de copia al sincronizar y al volver a publicarse; no se reemplaza esa procedencia por el último usuario que la compartió. Los valores locales `PRIVATE`/`PUBLIC` y `IMPORT`/`FORK` se traducen a minúsculas sólo en la frontera PostgreSQL para respetar las restricciones Android. El compositor adopta el límite Android de 280 caracteres. El Hub diferencia una comunidad realmente vacía de un error de red, permite reintentar sin descartar datos ya cargados y representa explícitamente una publicación retirada del buzón, evitando presentar fallos o eliminaciones como contenido inexistente normal.

## D-047 — Eliminar cuenta también elimina su caché local

Cerrar sesión conserva la caché aislada para una futura sesión offline, pero eliminar una cuenta es irreversible y debe retirar también sus datos asociados del dispositivo. La Edge Function elimina primero el usuario remoto para evitar perder la única copia antes de confirmar el servidor. Sólo tras ese éxito, iOS borra todas las entidades Core Data y preferencias con el `ownerId` confirmado, limpia Keychain y muestra confirmación; las filas de invitado y de otras cuentas no se alteran. Esta separación satisface la expectativa de eliminación completa sin convertir un simple cierre de sesión en pérdida de datos.

## D-048 — El favorito elegido gobierna el borrador de preparación

Guardar presets no basta para expresar cuál prefiere usar la persona. Android e iOS conservan por separado el identificador del último favorito seleccionado y lo restauran con prioridad al reabrir; al eliminar ese preset también eliminan la selección. Cada cambio numérico válido de la Calculadora actualiza dosis, agua, ratio, método y pasos del borrador de Preparación, por lo que cambiar de pestaña no requiere pulsar un botón de transferencia. Una preparación cuyo cronómetro ya comenzó nunca se reemplaza. En iOS el lienzo claro adopta los tokens canónicos `#F7F5F0`, `#EFECE6`, `#FFFFFF`, `#E6DFD5`, `#C26638` y sombra `#1E1A17` al 8%; el terracota usado como texto se oscurece sólo donde WCAG AA exige contraste.

## D-049 — El destinatario directo se elige por alias, no por UUID

Un UUID es una referencia interna estable, no una credencial que una persona deba copiar para usar el Hub. El compositor acepta el alias público con o sin `@`; una RPC autenticada realiza una coincidencia exacta sin distinguir mayúsculas, no devuelve la propia cuenta y sólo expone al cliente el UUID necesario para escribir `target_user_id`. Los alias activos se hacen únicos de forma insensible a mayúsculas. La política de lectura de perfiles continúa limitada y la pantalla de cuenta deja de mostrar su UUID técnico.
