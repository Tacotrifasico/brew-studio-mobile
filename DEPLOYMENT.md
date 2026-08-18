# Despliegue

## Configuración local

1. Copiar los valores públicos de Supabase a `Config/Development.xcconfig` para desarrollo y a `Config/Production.xcconfig` para distribución.
2. Usar la URL del proyecto y una **publishable key** (o la anon key heredada), nunca una secret key ni `service_role` en el proyecto iOS.
3. Mantener los archivos reales con secretos de servidor fuera del repositorio. `GEMINI_API_KEY` sólo se configura como secreto de Edge Functions.

Variables esperadas:

```text
SUPABASE_URL=https://PROJECT.supabase.co
SUPABASE_ANON_KEY=sb_publishable_...
```

## Supabase

Con Supabase CLI autenticado y enlazado al proyecto:

```bash
supabase link --project-ref PROJECT_REF
supabase db push
supabase secrets set GEMINI_API_KEY=VALUE GEMINI_MODEL=gemini-3.5-flash
supabase functions deploy gemini-suggestions
supabase functions deploy delete-account
```

Después se debe validar con dos cuentas distintas que cada usuario sólo pueda leer y modificar sus filas. También se prueban registro, verificación de correo, recuperación, renovación, cierre, eliminación, cuota de Gemini y restauración en otro dispositivo.

## Xcode y TestFlight

1. Abrir `Cupa.xcodeproj`.
2. Seleccionar el equipo Apple correcto en Signing & Capabilities y comprobar que el bundle ID `com.tacotrifasico.cupa` pertenece a ese equipo.
3. Configurar versión y build, iconos, privacidad, soporte y metadatos.
4. Ejecutar pruebas, un build Release y después Product → Archive con un destino iOS genérico.
5. Validar el Archive y subirlo a App Store Connect; probar primero mediante TestFlight interno.

La compilación sin firma ya está validada. El Archive firmado requiere certificado y perfil válidos en esta Mac.

Referencias oficiales: [claves Supabase](https://supabase.com/docs/guides/getting-started/api-keys), [autenticación en Edge Functions](https://supabase.com/docs/guides/functions/auth-legacy-jwt), [despliegue de funciones](https://supabase.com/docs/guides/functions/deploy) y [Gemini API](https://ai.google.dev/api).
