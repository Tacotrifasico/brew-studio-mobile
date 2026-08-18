# Matriz de paridad

Estados permitidos: No iniciado, Parcial, Implementado sin validar, Validado, Bloqueado.

| Módulo | Función | Referencia | Estado iOS | Datos | Visual | Pruebas | Bloqueo | Criterio de aceptación | Estado final |
|---|---|---|---|---|---|---|---|---|---|
| Navegación | Cinco secciones principales | `MainActivity.kt` | Implementado sin validar | N/A | Parcial | Compilación | Ninguno | Navegar sin perder estado | Implementado sin validar |
| Calculadora | Cálculo café → agua | `BaristaCalcViewModel.onCoffeeChanged` | Implementado sin validar | Local | Parcial | Pendiente | Ninguno | Mismos resultados y truncamiento | Implementado sin validar |
| Calculadora | Cálculo ratio → agua | `onRatioChanged` | Implementado sin validar | Local | Parcial | Pendiente | Ninguno | Paridad para límites y decimales | Implementado sin validar |
| Calculadora | Cálculo agua → café | `onWaterChanged` | Implementado sin validar | Local | Parcial | Pendiente | Ninguno | Mismo redondeo a un decimal | Implementado sin validar |
| Calculadora | Métodos y ratios base | `baseRatios` | Implementado sin validar | Local | Parcial | Pendiente | Ninguno | Siete métodos equivalentes | Implementado sin validar |
| Calculadora | Presets y favoritos | `defaultPresets`, Room | Parcial | UserDefaults | Parcial | Pendiente | Ninguno | Persistencia y edición equivalentes | Parcial |
| Calculadora | Transferencia a Preparación | `onActionPrepare` | Parcial | Memoria | Parcial | Pendiente | Ninguno | Todos los parámetros y pasos transferidos | Parcial |
| Calculadora | Transferencia a Laboratorio | `onActionLab` | Parcial | Memoria | Parcial | Pendiente | Ninguno | Estado compartido exacto | Parcial |
| Laboratorio | Motor sensorial continuo | `calculateLabProfile` | Parcial | Local | Parcial | Pendiente | Ninguno | Golden tests con fórmula exacta | Parcial |
| Laboratorio | Altitud y hervor | `LabAltitudeHeaderCard.kt` | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | Ciudades, altitud y límite térmico equivalentes | No iniciado |
| Laboratorio | Celsius/Fahrenheit | `LabScreen.kt` | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | Conversión y preferencias persistentes | No iniciado |
| Laboratorio | Persistir experimento | Room `LabExperiment` | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | Reabrir experimento tras reinicio | No iniciado |
| Preparación | Técnica y pasos | `BrewScreen.kt` | Parcial | Memoria | Parcial | Pendiente | Ninguno | Secuencia real completa | Parcial |
| Preparación | Cronómetro | ViewModel Android | Parcial | Memoria | Parcial | Pendiente | Ninguno | Pausa, reanuda, reinicia y recupera estado | Parcial |
| Cafés | Crear y listar | `StorageScreen.kt`, Room | Implementado sin validar | Core Data | Parcial | Compilación iPhone/simulador | Validar relanzamiento en simulador | Datos reales sobreviven reinicio | Implementado sin validar |
| Cafés | Editar y borrado lógico | Android Room | Implementado sin validar | Core Data | Parcial | Compilación iPhone/simulador | Pruebas de historial pendientes | Historial no se corrompe | Implementado sin validar |
| Molinos | CRUD | Android Room | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | CRUD persistente y sincronizable | No iniciado |
| Equipos | CRUD | Android Room | No iniciado | Ninguno | No iniciado | Pendiente | Ninguno | CRUD persistente y sincronizable | No iniciado |
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
