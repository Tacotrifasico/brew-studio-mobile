# Matriz de paridad

Estados permitidos: No iniciado, Parcial, Implementado sin validar, Validado, Bloqueado.

| Módulo | Función | Referencia | Estado iOS | Datos | Visual | Pruebas | Bloqueo | Criterio de aceptación | Estado final |
|---|---|---|---|---|---|---|---|---|---|
| Navegación | Cinco secciones principales | `MainActivity.kt` | Implementado sin validar | N/A | Parcial | Compilación | Ninguno | Navegar sin perder estado | Implementado sin validar |
| Calculadora | Cálculo café → agua | `BaristaCalcViewModel.onCoffeeChanged` | Validado | Local | Parcial | XCTest compilado + prueba de paridad | Ninguno | Mismos resultados y truncamiento | Validado |
| Calculadora | Cálculo ratio → agua | `onRatioChanged` | Validado | Local | Parcial | XCTest compilado + prueba de paridad | Ninguno | Paridad para límites y decimales | Validado |
| Calculadora | Cálculo agua → café | `onWaterChanged` | Validado | Local | Parcial | XCTest compilado + prueba de paridad | Ninguno | Mismo redondeo a un decimal | Validado |
| Calculadora | Métodos y ratios base | `baseRatios` | Validado | Local | Parcial | Siete métodos cubiertos por XCTest | Ninguno | Siete métodos equivalentes | Validado |
| Calculadora | Presets y favoritos | `defaultPresets`, Room | Parcial | UserDefaults | Parcial | Pendiente | Ninguno | Persistencia y edición equivalentes | Parcial |
| Calculadora | Transferencia a Preparación | `onActionPrepare` | Parcial | Memoria | Parcial | Pendiente | Ninguno | Todos los parámetros y pasos transferidos | Parcial |
| Calculadora | Transferencia a Laboratorio | `onActionLab` | Implementado sin validar | UserDefaults | Parcial | Compilación | Flujo UI pendiente | Estado compartido exacto | Implementado sin validar |
| Laboratorio | Motor sensorial continuo | `calculateLabProfile` | Validado | Local | Parcial | 4 golden tests ejecutados | Ninguno | Golden tests con fórmula exacta | Validado |
| Laboratorio | Altitud y hervor | `LabAltitudeHeaderCard.kt` | Implementado sin validar | UserDefaults | Implementado sin validar | Golden CDMX + límites | Ejecución UI pendiente por CoreSimulator | Ciudades, altitud y límite térmico equivalentes | Implementado sin validar |
| Laboratorio | Celsius/Fahrenheit | `LabScreen.kt` | Implementado sin validar | UserDefaults | Implementado sin validar | Conversión y restauración ejecutadas | Ejecución UI pendiente por CoreSimulator | Conversión y preferencias persistentes | Implementado sin validar |
| Laboratorio | Persistir experimento | Room `LabExperiment` | Implementado sin validar | Core Data | Implementado sin validar | CRUD en memoria ejecutado | Reapertura UI pendiente | Reabrir experimento tras reinicio | Implementado sin validar |
| Preparación | Técnica y pasos | `BrewScreen.kt` | Parcial | Memoria | Parcial | Pendiente | Ninguno | Secuencia real completa | Parcial |
| Preparación | Cronómetro | ViewModel Android | Parcial | Memoria | Parcial | Pendiente | Ninguno | Pausa, reanuda, reinicia y recupera estado | Parcial |
| Cafés | Crear y listar | `StorageScreen.kt`, Room | Implementado sin validar | Core Data | Parcial | Compilación iPhone/simulador | Validar relanzamiento en simulador | Datos reales sobreviven reinicio | Implementado sin validar |
| Cafés | Editar y borrado lógico | Android Room | Implementado sin validar | Core Data | Parcial | Compilación iPhone/simulador | Pruebas de historial pendientes | Historial no se corrompe | Implementado sin validar |
| Molinos | CRUD | `Instrument`, `GrinderProfile` | Implementado sin validar | Core Data | Implementado sin validar | CRUD ejecutado + test target compilado | Flujo UI pendiente por CoreSimulator | CRUD persistente y sincronizable | Implementado sin validar |
| Equipos | CRUD | Android `Instrument` | Implementado sin validar | Core Data | Implementado sin validar | CRUD ejecutado + test target compilado | Flujo UI pendiente por CoreSimulator | CRUD persistente y sincronizable | Implementado sin validar |
| Recetas | CRUD, ingredientes y pasos | Android Room | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | Flujo offline completo | No iniciado |
| Técnicas | CRUD y orden de pasos | Android Room | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | Entidad independiente y ejecutable | No iniciado |
| Cata | Perfil y notas | `CataScreen.kt` | Parcial | Memoria | Parcial | Pendiente | Ninguno | Guardar/editar sesión real | Parcial |
| Cata | Evolución de enfriamiento | Android ViewModel | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | Temporizador, etapas y recuperación | No iniciado |
| Cuenta | Registro/login/sesión | Supabase Android | No iniciado | Ninguno | No iniciado | Pendiente | URL/anon key | Sesión recuperable y aislada | Bloqueado |
| Sincronización | Offline → Supabase | Repositorios Android | No iniciado | Ninguno | N/A | Pendiente | URL/anon key | Reintentos y conflictos deterministas | Bloqueado |
| Social | Perfil y Brew Hub | Android social | No iniciado | Ninguno | No iniciado | Pendiente | URL/anon key | Datos reales y RLS | Bloqueado |
| Gemini | Sugerencias seguras | `GeminiService.kt` | No iniciado | Ninguno | No iniciado | Pendiente | Backend/secretos | Edge Function autenticada con fallback | Bloqueado |
| Tema | Claro y oscuro | Tema Android | Parcial | Preferencia del sistema | Parcial | Pendiente | Ninguno | Contraste y todos los componentes | Parcial |
| Distribución | Release y Archive | Xcode | No iniciado | N/A | N/A | Pendiente | Equipo Apple | Archive sin errores técnicos | Bloqueado |
