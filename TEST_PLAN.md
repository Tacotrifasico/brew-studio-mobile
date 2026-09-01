# Plan de pruebas

## Unitarias

- Calculadora: las tres direcciones, ratios base, decimales, coma, mínimos, truncamiento y redondeo.
- Laboratorio: extracción, altitud, hervor, tiempo, seis puntuaciones y límites.
- Frescura, temporizadores, mapeadores, conflictos y repositorios.

## Integración

- CRUD Core Data tras cerrar/reabrir.
- Login, recuperación de sesión y CRUD Supabase.
- Outbox offline, reconexión y conflicto.
- RLS con dos usuarios.
- Gemini, timeout, cuota, ausencia de red y fallback.

## Interfaz

- Cuenta, café, receta, técnica, preparación, Laboratorio, cata y enfriamiento.
- Reapertura, edición, eliminación, estados vacíos, errores y offline.
- El target `CupaUITests` arranca con Core Data en memoria y cubre navegación principal, campos de calculadora, acceso a preparación, Laboratorio, Almacén, Cata, Configuración y Brew Hub.

## Distribución

- Build Debug/Release, Archive, permisos, secretos, accesibilidad y ejecución sin red.

## Evidencia ejecutada

- `LabGoldenVerifier`: cuatro perfiles dorados aprobados (nivel del mar, CDMX/hervor, subextracción y sobreextracción).
- Calculadora: favorito personalizado guardado, restaurado desde `UserDefaults` aislado y eliminado: aprobado.
- Calculadora: restauración de método/café/ratio/agua y pasos rápidos V60, AeroPress y Espresso: aprobados.
- Calculadora: cuatro métodos fijados iniciales, personalización persistente, método de equipo con ratio de respaldo 1:15 y conservación de su UUID en favorito, Laboratorio y Preparación: verificador ejecutado + XCTest y recorrido del gestor compilados.
- Restauración de altitud, unidad y tiempo mediante `UserDefaults`: aprobada.
- Laboratorio geográfico: las 12 ciudades y su orden/altitud coinciden con `LabAltitudeHeaderCard.kt`; selección ciudad frente a manual, límites negativos/superiores a 5,000 m y hervor de 83 °C a 5,000 m ejecutados.
- Laboratorio térmico: conversión reversible, hervor CDMX mostrado como 198 °F, migración de la preferencia anterior y clave compartida con Configuración ejecutados; recorrido UI de ciudad personalizada y selector °C/°F compilado.
- Frescura de café: cortes exactos en 7/21/35/60 días, interpolación hasta 100%, fecha futura, ausencia de fecha y alerta tras 14 días abierta: aprobados.
- Alta, consulta y borrado lógico de café y molino; alta y actualización de equipo; alta de experimento en Core Data en memoria: aprobados.
- Persistencia local real: café con UUID, fecha, origen y existencias guardado en SQLite, store desmontado y recuperado desde un segundo `NSPersistentContainer`: aprobado.
- Receta: alta, edición por UUID, reordenamiento, favorita, duplicación profunda y borrado lógico de hijos: aprobados.
- Recetario: importación local de texto libre, inferencia de categoría/método, unidades, pasos y valores de respaldo equivalentes al contrato Android: verificador ejecutado + XCTest compilado.
- Recetario: detalle legible con intención, etiquetas, cantidades, unidades, duraciones y acciones de favorita/duplicar/editar/eliminar; acceso a alta e importación cubierto por el target UI compilado.
- Técnica: alta, detalle completo, edición, reordenamiento, eliminación de pasos, duración total, agua acumulada, gesto, intensidad, cobertura, flujo, acción secundaria, borrado lógico y carga directa en Preparación: verificador ejecutado + XCTest y acceso UI compilados.
- Preparación: carga desde técnica, avance guiado tras 46 segundos, cierre en tiempo total, recuperación pendiente de guardado, prevención de duplicados, nuevo UUID y snapshot persistente de `BrewSession`: aprobados.
- Cata: cortes térmicos exactos a 0/239/240/599/600/959/960 segundos, pausa, recuperación a 601 segundos, reinicio, alta/baja de observaciones, cierre inmutable, vínculo opcional con `BrewSession`, detalle/historial íntegro, edición con el mismo UUID, reconciliación sin duplicar `CupSession`, alta posterior con UUID nuevo y borrado lógico conjunto: verificador ejecutado + XCTest y UI compilados.
- Molinos y equipos: alta, lectura íntegra, edición conservando UUID, validación de rango/capacidad, reapertura desde un contenedor SQLite nuevo, estados de sincronización, borrado lógico y conservación de referencias/snapshots en preparaciones históricas: verificador ejecutado + XCTest y recorridos UI compilados.
- Tazas: referencias, dosis, agua, ratio, temperatura, molienda, duración, cinco nombres congelados, vida térmica, valoración, NPS, comentario y fecha verificados contra la preparación/cata de origen; detalle completo y eliminación conjunta confirmada compilados: aprobados.
- Cuenta: estado seguro sin configuración, contrato GoTrue, sesión vencida sin red conservada y refresh rechazado limpiado: casos críticos ejecutados en el verificador + XCTest compilado.
- Eliminación de cuenta: ruta autenticada, confirmación explícita y ausencia de credenciales administrativas en el request iOS: XCTest compilado.
- Sincronización: elección por fecha/versión, rechazo de propietario distinto, compactación de outbox, reintento exponencial y finalización: verificador ejecutado.
- Sincronización integral: asignación de propietario a datos offline, JSON/JSONB, push, pull incremental, merge, checkpoint, outbox preservada sin red y reintento automático al reconectar: XCTest compilado + mapeo ejecutado.
- Gemini: llamada autenticada a Edge Function, validación de respuesta y fallback local determinista cuando no hay configuración: verificador ejecutado + XCTest compilado.
- Perfil: creación, edición con UUID estable, normalización de alias, aislamiento entre dos propietarios y DTO sin correo: verificador ejecutado.
- Configuración: restauración de tema oscuro y Fahrenheit aprobada; eliminado el control métrico sin efecto que no existe en Android.
- Navegación: las cinco secciones mantienen el orden Taller, Preparar, Cata, Laboratorio y Almacén; el modelo compartido conserva los 18 g introducidos en Calculadora al recorrerlas todas: XCTest y recorrido UI compilados.
- Tema y accesibilidad: paletas clara y oscura verificadas matemáticamente con contraste WCAG AA mínimo 4.5:1 para texto, secundarios, acentos y categorías; colores sobre botones adaptativos, métricas críticas con Dynamic Type y fila de valoración refluible con valor ajustable: XCTest compilado.
- Inventario: fallos de guardado de café, molino y equipo hacen rollback, mantienen el editor abierto y muestran alerta; flujo compilado.
- Café: altitud fuera de rango, cantidades vacías/no numéricas/negativas, existencias mayores al lote y fechas futuras o invertidas son rechazadas antes de guardar; decimal con coma aceptado: verificador ejecutado + XCTest compilado.
- Recuperación de Core Data: un destino SQLite imposible activa un store temporal visible, permite guardar durante la sesión y no termina la app ni destruye el archivo original: verificador ejecutado + XCTest compilado.
- Acciones destructivas: café, taza/cata, experimento, receta, técnica y reinicios con progreso sólo se ejecutan después de una confirmación explícita; se retiraron borrados por deslizamiento que la omitían: targets UI compilados.
- Historial por café: consulta de preparación y taza por `beanId`, conteos, calificación/comentario y conservación de ambas sesiones después del borrado lógico del café: verificador ejecutado + XCTest compilado.
- Acciones de café: transición cerrado/abierto/terminado, fecha de apertura, separación de lotes terminados, UUID recuperable en Preparación y carga de frescura/proceso/notas al Laboratorio: verificador ejecutado + XCTest compilado.
- Social: feed público, publicación sin correo, envío directo con destinatario, buzón/lectura, actividad, Me gusta/guardados, reporte, bloqueo y filtro preventivo: contratos XCTest compilados. Copia y variante profundas con UUID nuevos, atribución y modos `IMPORT`/`FORK`: verificador ejecutado.
- Target `CupaTests`: compilación para iOS Simulator aprobada. La ejecución XCTest queda pendiente hasta reparar CoreSimulator local.
- Target `CupaUITests`: cinco recorridos XCTest UI compilados para arm64 y x86_64. La ejecución queda pendiente hasta reparar CoreSimulator local.
- Build Release para iPhone genérico sin firma: aprobado.
- Accesibilidad estática: controles de sólo icono etiquetados, áreas táctiles de 44 puntos e identificadores para flujos críticos: compilado.
- Privacidad: `PrivacyInfo.xcprivacy` válido con `plutil` e incluido en el bundle Release; consentimiento Gemini explícito y revocable: compilado.
- Archive técnico Release: aprobado sin firma, excluyendo únicamente AppIcon por fallo de `CoreSimulatorService/simdiskimaged` que afecta a `actool` en esta Mac.
- Metadatos Release: bundle `com.tacotrifasico.cupa`, versión `1.0` y `ITSAppUsesNonExemptEncryption=false` comprobados en el `Info.plist` construido; enlaces HTTPS legales aparecen sólo cuando están configurados.
- Perfil/UGC: normalización y rechazo de entradas inválidas, política privado/directo, categorías de reporte y contratos de listar/desbloquear compilados; el verificador ejecuta persistencia, color normalizado y política de visibilidad.
- Ambientes: Xcode detecta Debug, Staging y Release; los esquemas compartidos `Cupa` y `Cupa-Staging` están disponibles. Staging compila app, unitarias y cinco recorridos UI para ambas arquitecturas de simulador con nombre/bundle independientes; Production compila para iPhone con `APP_ENVIRONMENT=Production` dentro del bundle.
- Configuración remota: valores vacíos e HTTP remoto se rechazan, HTTPS se normaliza y HTTP local se admite sólo para `localhost`/`127.0.0.1`: verificador ejecutado + XCTest compilado.
- Distribución estática: launch screen nativo, español como región de desarrollo, iPhone arm64, pantalla completa y orientaciones soportadas constan en el `Info.plist`; no se importan APIs que requieran cámara, fotos, ubicación, micrófono, contactos, calendario o salud.

Comando alternativo de verificación cuando CoreSimulator no inicia:

```bash
MAC_SDK=$(DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun --sdk macosx --show-sdk-path)
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/swiftc -target arm64-apple-macosx15.0 -sdk "$MAC_SDK" -module-cache-path /private/tmp/cupa-swift-module-cache -framework Combine -framework CoreData -framework Security Cupa/CalculatorModel.swift Cupa/LabModel.swift Cupa/PreparationModel.swift Cupa/RecipeTechniqueModels.swift Cupa/TastingModels.swift Cupa/ProfileModels.swift Cupa/AccountServices.swift Cupa/SyncEngine.swift Cupa/EntitySyncCoordinator.swift Cupa/SuggestionServices.swift Cupa/SocialModels.swift Cupa/PersistenceModels.swift Cupa/RecipeTechniqueRepository.swift Tools/LabGoldenVerifier.swift -o /private/tmp/cupa-domain-verifier
/private/tmp/cupa-domain-verifier
```

El verificador también cubre Calculadora → Laboratorio, Receta/Técnica/Inventario → Laboratorio, Laboratorio → Preparación, Cata → Laboratorio, restauración desde `UserDefaults`, reapertura y borrado lógico de experimentos, persistencia de referencias y conservación de snapshots después del borrado lógico del inventario.
