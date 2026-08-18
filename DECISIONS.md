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
