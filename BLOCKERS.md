# Bloqueos externos reales

| Bloqueo | Impacto | Dato requerido | Trabajo que continúa |
|---|---|---|---|
| Supabase no configurado | Login, sincronización y RLS no pueden validarse contra producción | `SUPABASE_URL` y `SUPABASE_ANON_KEY` reales; nunca `service_role` | Modelos, repositorios, cola offline y pruebas con dobles |
| Backend Gemini no desplegado | No se pueden validar sugerencias remotas | Proyecto Supabase y secreto Gemini en Edge Function | Contrato, fallback local y cliente autenticado |
| Equipo Apple no seleccionado | Archive firmado/TestFlight no puede completarse | Apple Developer Team en Xcode | Build sin firma, validación de proyecto y checklist |
| Información legal pendiente | Publicación final | URL de privacidad, soporte, política de eliminación y clasificación | Preparación técnica y campos documentados |
| Firma local de Xcode no confiable | SwiftDataMacros y algunos servicios internos de Xcode fallan | Reparar/reinstalar Xcode oficial y aceptar sus componentes | La app compila con Core Data; builds genéricos siguen validados |
