# Bloqueos externos reales

| Bloqueo | Impacto | Dato requerido | Trabajo que continúa |
|---|---|---|---|
| Supabase no configurado | Login, sincronización, migración Android/iOS y RLS no pueden validarse contra datos reales | `SUPABASE_URL` y `SUPABASE_ANON_KEY` reales; acceso CLI a un clon Staging y respaldo antes de aplicar migraciones; nunca `service_role` | Contrato compartido, migraciones sintácticamente válidas, repositorios, cola offline y pruebas con dobles |
| Backend Gemini no desplegado | No se pueden validar sugerencias remotas | Proyecto Supabase y secreto Gemini en Edge Function | Contrato, fallback local y cliente autenticado |
| Certificado/perfil Apple no disponible | Archive firmado/TestFlight no puede completarse aunque el Team ID está configurado | Certificado Apple Development/Distribution y perfil válidos para el equipo `6UD7WV66N5` | Build sin firma, validación de proyecto y checklist |
| Información legal pendiente | Publicación final | URLs HTTPS de privacidad, soporte/moderación y normas de comunidad; texto de retención/eliminación, clasificación y contacto responsable | Variables y enlaces dentro de la app preparados |
| CoreSimulator no accesible desde el entorno aislado de Codex | `simctl` no puede comunicarse con `CoreSimulatorService` aunque el simulador sí abre desde Xcode para el usuario; impide ejecutar UI y `actool` dentro de esta tarea | Ejecutar los recorridos desde Xcode o permitir una sesión de automatización con acceso al servicio de Simulator | Release y Archive técnico se validan excluyendo sólo el catálogo; AppIcon y targets UI ya están preparados |
| GitHub no autenticado para Terminal | La red funciona y `origin/main` es legible, pero Git rechaza el push HTTPS porque no existe una credencial de usuario | Iniciar sesión una vez con GitHub Desktop/Git Credential Manager y ejecutar `git push -u origin ios-production-migration` | El historial local y los paquetes ZIP verificables siguen avanzando |
