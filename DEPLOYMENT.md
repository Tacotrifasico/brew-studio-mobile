# Despliegue

## Configuración local

1. Copiar los valores públicos de Supabase a `Config/Development.xcconfig` para desarrollo y a `Config/Production.xcconfig` para distribución.
2. Usar la URL del proyecto y una **publishable key** (o la anon key heredada), nunca una secret key ni `service_role` en el proyecto iOS.
3. Mantener los archivos reales con secretos de servidor fuera del repositorio. `GEMINI_API_KEY` sólo se configura como secreto de Edge Functions.

Variables esperadas:

```text
SUPABASE_URL=https://PROJECT.supabase.co
SUPABASE_ANON_KEY=sb_publishable_...
PRIVACY_POLICY_URL=https://ejemplo.com/privacidad
```

En archivos `.xcconfig`, una URL con `//` debe escaparse para que no se interprete como comentario, por ejemplo `PRIVACY_POLICY_URL = https:/$()/ejemplo.com/privacidad`.

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
3. Configurar versión y build, confirmar el AppIcon incluido, definir `PRIVACY_POLICY_URL`, soporte, clasificación por edad y metadatos.
4. Ejecutar pruebas, un build Release y después Product → Archive con un destino iOS genérico.
5. Validar el Archive y subirlo a App Store Connect; probar primero mediante TestFlight interno.

La compilación sin firma y un Archive técnico ya están validados. El Archive firmado requiere certificado y perfil válidos. En esta instalación también debe repararse `CoreSimulatorService/simdiskimaged`: actualmente `actool` no puede descubrir runtimes y falla al compilar el catálogo AppIcon incluso para iPhone genérico.

Apple exige que la política de privacidad esté disponible tanto en App Store Connect como dentro de la app, que las prácticas de datos se declaren en App Privacy y que contenido social permita reportar y bloquear. La app ya implementa reporte, bloqueo, eliminación de cuenta, manifiesto de privacidad y consentimiento revocable antes de compartir parámetros con Gemini; el texto legal, URL pública, contacto de moderación y respuestas de App Privacy deben ser definidos por el propietario.

Referencias oficiales: [claves Supabase](https://supabase.com/docs/guides/getting-started/api-keys), [autenticación en Edge Functions](https://supabase.com/docs/guides/functions/auth-legacy-jwt), [despliegue de funciones](https://supabase.com/docs/guides/functions/deploy), [Gemini API](https://ai.google.dev/api), [App Review](https://developer.apple.com/app-store/review/guidelines/), [manifiestos de privacidad](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files) y [subida de builds](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds).
