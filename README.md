# Cupa para iOS

Migración nativa en curso de Brew Studio Mobile a iPhone.

## Abrir

1. Abre `Cupa.xcodeproj` con Xcode.
2. En la parte superior, elige un simulador de iPhone.
3. Pulsa el botón triangular **Run**.

Esta etapa incluye navegación principal, diseño visual base, calculadora bidireccional portada desde Android, favoritos locales, formulario de cata, Laboratorio con fórmula sensorial exacta, altitud, hervor, °C/°F y experimentos persistentes, además del CRUD offline de cafés, molinos, equipos, recetas y técnicas con Core Data y borrado lógico. La preparación ya ejecuta técnicas reales con cronómetro guiado o manual, pausa, reanudación, reinicio, recuperación tras cierre y guardado de sesiones históricas.

El target `CupaTests` contiene pruebas XCTest. Si CoreSimulator no inicia, `Tools/LabGoldenVerifier.swift` valida cuatro casos dorados, restauración de estado y persistencia Core Data desde macOS; el comando está en `TEST_PLAN.md`.

Supabase, sincronización multiusuario, Gemini mediante backend y los demás CRUD siguen el estado documentado en `PARITY_MATRIX.md` y `BLOCKERS.md`.
