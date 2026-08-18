# Arquitectura iOS

```text
SwiftUI Views
    ↓ acciones / estado observable
Feature ViewModels
    ↓ protocolos
Domain Engines ──────→ Calculator / Lab / Freshness
    ↓
Repositories
    ├── Core Data cache + outbox offline + retry
    └── Supabase Auth/REST + Keychain session
             └── Edge Function Gemini
```

## Capas

- `UI`: vistas SwiftUI, navegación, accesibilidad y estados de carga/error/vacío/offline.
- `Presentation`: estado por módulo y coordinación de flujos.
- `Domain`: modelos independientes y motores matemáticos deterministas.
- `Data`: protocolos de repositorio, Core Data, DTO remotos, mapeadores y sincronización.
- `Services`: autenticación, Supabase, Gemini, configuración y telemetría no sensible.

Las vistas no realizan consultas HTTP ni contienen fórmulas de negocio. Las entidades de dominio no dependen de SwiftUI.

`AccountModel` coordina la sesión y `SupabaseAuthService` implementa el contrato GoTrue mediante un transporte inyectable. `SyncOutboxRepository` compacta mutaciones locales y `SupabaseDataService` prepara upsert, borrado lógico y descarga incremental. Development y Production usan archivos `.xcconfig`; Staging queda listo para asociarse a un esquema cuando se configure el proyecto remoto.
