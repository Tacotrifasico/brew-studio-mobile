# Despliegue

## Configuración local

1. Guardar los valores públicos de Supabase en el archivo local del ambiente: `Config/Development.local.xcconfig`, `Config/Staging.local.xcconfig` o `Config/Production.local.xcconfig`. Están ignorados por Git y cada archivo de ambiente versionado incluye opcionalmente su variante local al final.
2. Usar la URL del proyecto y una **publishable key** (o la anon key heredada), nunca una secret key ni `service_role` en el proyecto iOS.
3. Mantener secretos de servidor fuera del repositorio y del cliente. Brew Studio no usa IA: no configurar claves de Google ni desplegar funciones Gemini heredadas.

Variables esperadas:

```text
SUPABASE_URL=https://PROJECT.supabase.co
SUPABASE_ANON_KEY=sb_publishable_...
AUTH_URL_SCHEME=com.tacotrifasico.cupa
PRIVACY_POLICY_URL=https://ejemplo.com/privacidad
SUPPORT_URL=https://ejemplo.com/soporte
```

En archivos `.xcconfig`, una URL con `//` debe escaparse para que no se interprete como comentario, por ejemplo `SUPABASE_URL = https:/$()/PROJECT.supabase.co`, `PRIVACY_POLICY_URL = https:/$()/ejemplo.com/privacidad` y `SUPPORT_URL = https:/$()/ejemplo.com/soporte`.

## Supabase

Con Supabase CLI autenticado y enlazado al proyecto:

```bash
supabase link --project-ref PROJECT_REF
supabase db push --dry-run
supabase db push
supabase functions deploy delete-account
```

Antes de `supabase db push`, crear un respaldo y ensayar sobre un proyecto Staging clonado. El orden versionado prepara primero las tablas Android, conserva las referencias de granos y finalmente alinea `beans`/`shares` como fuentes comunes. Después del ensayo, comparar conteos y una muestra por UUID antes/después; no ejecutar la migración directamente sobre producción si aparecen violaciones de claves, propietarios nulos o duplicados de buzón.

Después se debe validar con dos cuentas distintas que cada usuario sólo pueda leer y modificar sus filas. También se prueban registro, verificación de correo, recuperación, renovación, cierre, eliminación y restauración en otro dispositivo. Con las dos aplicaciones se crea y edita al menos un café, molino, equipo, receta, técnica con pasos, experimento, perfil y publicación en cada dirección; ambos clientes deben ver el mismo UUID y el último valor.

Los archivos y migraciones históricos de IA no habilitan esa funcionalidad en el producto. No desplegar `gemini-suggestions` ni añadir llamadas a generación; los consejos del laboratorio se calculan localmente. No reescribir el historial de migraciones ya aplicado en un proyecto existente.

En Authentication → Email debe configurarse un proveedor SMTP real y conservarse **Confirm email** activado para producción. La app acepta correctamente tanto el alta que devuelve sesión inmediata como el alta que devuelve sólo usuario pendiente, permite reenviar la confirmación y pide iniciar sesión después de abrir el enlace. La recuperación usa una respuesta deliberadamente genérica para no revelar si una dirección tiene cuenta.

En Authentication → URL Configuration, conservar un `SITE_URL` HTTPS controlado y agregar exactamente estas Redirect URLs permitidas:

```text
com.tacotrifasico.cupa://auth/recovery
com.tacotrifasico.cupa.staging://auth/recovery
```

Production/Development registran el primer esquema y Staging registra el segundo, evitando que el correo de pruebas abra la aplicación productiva. El callback sólo acepta host `auth`, ruta `recovery`, tipo `recovery`, un token no vacío y una expiración vigente. El token queda únicamente en memoria durante el formulario, se usa para `PUT /auth/v1/user` y se descarta al cerrar o completar. Un enlace universal HTTPS sería más resistente a que otra app reclame el esquema; incorporarlo requiere el dominio controlado y su archivo de asociación antes de habilitar Associated Domains.

## Xcode y TestFlight

1. Abrir `Cupa.xcodeproj`.
2. Usar el esquema `Cupa` para Development/Production o `Cupa-Staging` para el backend de prueba. Staging usa `com.tacotrifasico.cupa.staging` y el nombre visible “Cupa Staging”, por lo que puede convivir con producción.
3. Seleccionar el equipo Apple correcto en Signing & Capabilities y comprobar que el bundle ID `com.tacotrifasico.cupa` pertenece a ese equipo.
4. Configurar versión y build, confirmar el AppIcon incluido, definir `PRIVACY_POLICY_URL` y `SUPPORT_URL`, clasificación por edad y metadatos.
5. Ejecutar pruebas, un build Release y después Product → Archive con un destino iOS genérico.
6. Validar el Archive y subirlo a App Store Connect; probar primero mediante TestFlight interno.

Desde el 28 de abril de 2026 Apple exige construir las entregas iOS con el SDK iOS 26 o posterior. El proyecto ya se compila con Xcode 26.6 y SDK iOS 26.5. La configuración `UILaunchScreen` también está presente, por lo que anticipa la validación anunciada para binarios construidos con el SDK iOS 27.

El `Info.plist` expande dentro del bundle el ambiente y sus valores públicos. Una URL Supabase remota sólo se acepta con HTTPS; HTTP se admite únicamente para `localhost` o `127.0.0.1` durante desarrollo local. Una configuración vacía o insegura mantiene la cuenta deshabilitada en vez de construir solicitudes inválidas.

La compilación sin firma y un Archive técnico ya están validados. El Archive firmado requiere certificado y perfil válidos. En esta instalación también debe repararse `CoreSimulatorService/simdiskimaged`: actualmente `actool` no puede descubrir runtimes y falla al compilar el catálogo AppIcon incluso para iPhone genérico.

`ITSAppUsesNonExemptEncryption` está en `NO` porque el código auditado sólo usa cifrado estándar provisto por iOS para HTTPS y Keychain; no incorpora algoritmos criptográficos propios. Confirmar esta declaración si se agrega posteriormente una biblioteca de cifrado.

La preparación de publicación debe incluir política de privacidad, declaración de datos, reporte y bloqueo de contenido social, eliminación de cuenta y contacto de moderación. Los textos legales, URLs públicas, proceso humano de moderación y respuestas de App Privacy deben ser definidos por el propietario y revisados contra los requisitos vigentes. No declarar uso de IA: el producto no lo incorpora. Para revisión se necesita una cuenta de demostración activa y el backend accesible.

Referencias oficiales: [claves Supabase](https://supabase.com/docs/guides/getting-started/api-keys), [recuperación por contraseña](https://supabase.com/docs/guides/auth/passwords), [enlaces profundos móviles](https://supabase.com/docs/guides/auth/native-mobile-deep-linking), [autenticación en Edge Functions](https://supabase.com/docs/guides/functions/auth-legacy-jwt), [despliegue de funciones](https://supabase.com/docs/guides/functions/deploy), [Gemini API](https://ai.google.dev/api), [App Review](https://developer.apple.com/app-store/review/guidelines/), [eliminación de cuentas](https://developer.apple.com/support/offering-account-deletion-in-your-app/), [requisitos de envío](https://developer.apple.com/app-store/submitting/), [manifiestos de privacidad](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files), [launch screen](https://developer.apple.com/documentation/technotes/tn3208-preparing-your-apps-launch-screen-to-meet-app-store-requirements) y [subida de builds](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds).
