# Bloqueos externos reales

| Bloqueo | Impacto | Dato requerido | Trabajo que continúa |
|---|---|---|---|
| Supabase no configurado | Login, sincronización, migración Android/iOS y RLS no pueden validarse contra datos reales | `SUPABASE_URL` y `SUPABASE_ANON_KEY` reales; acceso CLI a un clon Staging y respaldo antes de aplicar migraciones; nunca `service_role` | Contrato compartido, migraciones sintácticamente válidas, repositorios, cola offline y pruebas con dobles |
| Backend Gemini no desplegado | No se pueden validar sugerencias remotas | Proyecto Supabase y secreto Gemini en Edge Function | Contrato, fallback local y cliente autenticado |
| Certificado/perfil Apple no disponible | Archive firmado/TestFlight no puede completarse aunque el Team ID está configurado | Certificado Apple Development/Distribution y perfil válidos para el equipo `6UD7WV66N5` | Build sin firma, validación de proyecto y checklist |
| Información legal pendiente | Publicación final | URLs HTTPS de privacidad, soporte/moderación y normas de comunidad; texto de retención/eliminación, clasificación y contacto responsable | Variables y enlaces dentro de la app preparados |
| Servicios locales de Xcode dañados | CoreSimulator y `actool` no descubren runtimes; impide ejecutar UI y compilar el catálogo AppIcon en esta sesión | Reiniciar/reparar componentes de Xcode y runtimes oficiales | Release y Archive técnico se validan excluyendo sólo el catálogo; el AppIcon ya está preparado |
| Red hacia GitHub no disponible en esta sesión | El commit local no puede subirse al remoto | Resolver DNS/conectividad a `github.com` y ejecutar `git push -u origin ios-production-migration` | El historial local y los paquetes ZIP verificables siguen avanzando |
