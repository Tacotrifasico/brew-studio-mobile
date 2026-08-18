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

`AccountModel` coordina la sesión y `SupabaseAuthService` implementa GoTrue mediante un transporte inyectable. Los cortes transitorios conservan identidad y trabajo local; un refresh inválido limpia la sesión, y las operaciones autenticadas renuevan una vez ante `401`. `ConnectivityMonitor` reintenta al recuperar red. `SyncOutboxRepository` compacta mutaciones locales y `SupabaseDataService` prepara upsert, borrado lógico y descarga incremental. Development, Staging y Production usan configuraciones `.xcconfig` operativas. `Cupa` ejecuta Development y archiva Production; `Cupa-Staging` ejecuta, prueba y archiva Staging con bundle separado.

`SocialService` opera snapshots públicos o directos sin exponer correo. El buzón referencia esos snapshots y sólo persiste destinatario/lectura; copias y variantes se materializan mediante `RecipeTechniqueRepository` con UUID propios. Likes, guardados, bloqueos, reportes y actividad permanecen como recursos REST separados protegidos por RLS.
