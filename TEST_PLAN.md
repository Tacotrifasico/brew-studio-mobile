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

## Distribución

- Build Debug/Release, Archive, permisos, secretos, accesibilidad y ejecución sin red.

## Evidencia ejecutada

- `LabGoldenVerifier`: cuatro perfiles dorados aprobados (nivel del mar, CDMX/hervor, subextracción y sobreextracción).
- Restauración de altitud, unidad y tiempo mediante `UserDefaults`: aprobada.
- Alta, consulta y borrado lógico de café y molino; alta y actualización de equipo; alta de experimento en Core Data en memoria: aprobados.
- Receta: alta, edición por UUID, reordenamiento, duplicación profunda y borrado lógico de hijos: aprobados.
- Técnica: alta, orden, duración total, agua acumulada y borrado lógico de pasos: aprobados.
- Target `CupaTests`: compilación para iOS Simulator aprobada. La ejecución XCTest queda pendiente hasta reparar CoreSimulator local.

Comando alternativo de verificación cuando CoreSimulator no inicia:

```bash
MAC_SDK=$(DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun --sdk macosx --show-sdk-path)
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/swiftc -target arm64-apple-macosx15.0 -sdk "$MAC_SDK" -module-cache-path /private/tmp/cupa-swift-module-cache -framework Combine -framework CoreData Cupa/CalculatorModel.swift Cupa/LabModel.swift Cupa/RecipeTechniqueModels.swift Cupa/PersistenceModels.swift Cupa/RecipeTechniqueRepository.swift Tools/LabGoldenVerifier.swift -o /private/tmp/cupa-domain-verifier
/private/tmp/cupa-domain-verifier
```
