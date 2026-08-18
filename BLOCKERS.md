# Bloqueos externos reales

| Bloqueo | Impacto | Dato requerido | Trabajo que continúa |
|---|---|---|---|
| Supabase no configurado | Login, sincronización y RLS no pueden validarse contra producción | `SUPABASE_URL` y `SUPABASE_ANON_KEY` reales; nunca `service_role` | Modelos, repositorios, cola offline y pruebas con dobles |
| Backend Gemini no desplegado | No se pueden validar sugerencias remotas | Proyecto Supabase y secreto Gemini en Edge Function | Contrato, fallback local y cliente autenticado |
| Certificado/perfil Apple no disponible | Archive firmado/TestFlight no puede completarse aunque el Team ID está configurado | Certificado Apple Development/Distribution y perfil válidos para el equipo `6UD7WV66N5` | Build sin firma, validación de proyecto y checklist |
| Información legal pendiente | Publicación final | URLs HTTPS de privacidad y soporte/moderación, texto de retención/eliminación, clasificación y contacto responsable | Variables y enlaces dentro de la app preparados |
| Servicios locales de Xcode dañados | CoreSimulator y `actool` no descubren runtimes; impide ejecutar UI y compilar el catálogo AppIcon en esta sesión | Reiniciar/reparar componentes de Xcode y runtimes oficiales | Release y Archive técnico se validan excluyendo sólo el catálogo; el AppIcon ya está preparado |
