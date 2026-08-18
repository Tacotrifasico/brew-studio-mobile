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
- Restauración de altitud, unidad y tiempo mediante `UserDefaults`: aprobada.
- Alta, consulta y borrado lógico de café y molino; alta y actualización de equipo; alta de experimento en Core Data en memoria: aprobados.
- Receta: alta, edición por UUID, reordenamiento, duplicación profunda y borrado lógico de hijos: aprobados.
- Técnica: alta, orden, duración total, agua acumulada y borrado lógico de pasos: aprobados.
- Preparación: carga desde técnica, avance guiado tras 46 segundos, pausa, recuperación desde `UserDefaults` y snapshot persistente de `BrewSession`: aprobados.
- Cata: recuperación del enfriamiento a 601 segundos, transición a descenso, observación sensorial independiente, vínculo opcional con `BrewSession`, creación de `CupSession` y borrado lógico: aprobados.
- Cuenta: estado seguro sin configuración y contrato de login GoTrue (ruta, anon header y mapeo de tokens): XCTest compilado.
- Eliminación de cuenta: ruta autenticada, confirmación explícita y ausencia de credenciales administrativas en el request iOS: XCTest compilado.
- Sincronización: elección por fecha/versión, rechazo de propietario distinto, compactación de outbox, reintento exponencial y finalización: verificador ejecutado.
- Sincronización integral: asignación de propietario a datos offline, JSON/JSONB, push de pendientes, pull incremental de todos los descriptores, merge remoto y checkpoint: XCTest compilado + mapeo ejecutado.
- Gemini: llamada autenticada a Edge Function, validación de respuesta y fallback local determinista cuando no hay configuración: verificador ejecutado + XCTest compilado.
- Perfil: creación, edición con UUID estable, normalización de alias, aislamiento entre dos propietarios y DTO sin correo: verificador ejecutado.
- Configuración: restauración de tema oscuro, Fahrenheit y unidades: XCTest compilado.
- Social: contrato del feed público, publicación sin correo, reporte, bloqueo, filtro preventivo e importación profunda con atribución: verificador ejecutado + XCTest compilado.
- Target `CupaTests`: compilación para iOS Simulator aprobada. La ejecución XCTest queda pendiente hasta reparar CoreSimulator local.
- Target `CupaUITests`: tres recorridos XCTest UI compilados para arm64 y x86_64. La ejecución queda pendiente hasta reparar CoreSimulator local.
- Build Release para iPhone genérico sin firma: aprobado.
- Accesibilidad estática: controles de sólo icono etiquetados, áreas táctiles de 44 puntos e identificadores para flujos críticos: compilado.
- Privacidad: `PrivacyInfo.xcprivacy` válido con `plutil` e incluido en el bundle Release; consentimiento Gemini explícito y revocable: compilado.
- Archive técnico Release: aprobado sin firma, excluyendo únicamente AppIcon por fallo de `CoreSimulatorService/simdiskimaged` que afecta a `actool` en esta Mac.
- Metadatos Release: bundle `com.tacotrifasico.cupa`, versión `1.0` y `ITSAppUsesNonExemptEncryption=false` comprobados en el `Info.plist` construido; enlaces HTTPS legales aparecen sólo cuando están configurados.

Comando alternativo de verificación cuando CoreSimulator no inicia:

```bash
MAC_SDK=$(DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun --sdk macosx --show-sdk-path)
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/swiftc -target arm64-apple-macosx15.0 -sdk "$MAC_SDK" -module-cache-path /private/tmp/cupa-swift-module-cache -framework Combine -framework CoreData -framework Security Cupa/CalculatorModel.swift Cupa/LabModel.swift Cupa/PreparationModel.swift Cupa/RecipeTechniqueModels.swift Cupa/TastingModels.swift Cupa/ProfileModels.swift Cupa/AccountServices.swift Cupa/SyncEngine.swift Cupa/EntitySyncCoordinator.swift Cupa/SuggestionServices.swift Cupa/SocialModels.swift Cupa/PersistenceModels.swift Cupa/RecipeTechniqueRepository.swift Tools/LabGoldenVerifier.swift -o /private/tmp/cupa-domain-verifier
/private/tmp/cupa-domain-verifier
```
