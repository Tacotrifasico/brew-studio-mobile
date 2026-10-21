# cUpa

Repositorio principal de las aplicaciones nativas de cUpa. Android e iOS comparten
comportamiento, datos y diseño; cada plataforma conserva sus controles nativos.
Instagram oficial: [@cupa.brew](https://www.instagram.com/cupa.brew/).

## Código de ambas plataformas en main

- **Android:** proyecto Gradle en la raíz; código en `app/`. Abrir esta carpeta en
  Android Studio.
- **iOS:** proyecto SwiftUI en `ios/`. Abrir `ios/Cupa.xcodeproj` en Xcode.
- **Backend local-first:** instrucciones y migraciones canónicas en
  `ios/Backend/README.md`. No aplicar migraciones históricas de otras carpetas
  sobre el proyecto existente de Supabase.

La carpeta `ios/` corresponde a la publicación de `ios-production-migration`,
commit `7373aac8244713e4f1a6e31d0c992f927b1ae9ba`. Se conserva esa rama y su
historial; `main` permite consultar y descargar las dos aplicaciones juntas.
Los cambios futuros deben mantenerse alineados en ambas plataformas.

## Estado publicado

- Marca cUpa, nuevo ícono de instalación e Instagram oficial.
- Login obligatorio, Google con PKCE y sesiones guardadas de forma segura.
- Reingreso después de cerrar sesión corregido; sin acceso invitado ni demo.
- Almacén privado local. Supabase registra cuentas/perfiles y soporta publicaciones
  explícitas, intercambio, atribución y likes; no se sube la biblioteca al login.
- Una cuenta previamente autenticada conserva el uso de herramientas locales sin
  internet. La primera entrada y las funciones sociales requieren conexión.
- Sin IA dentro de la aplicación. «Sin cargos extras» describe cargos al usuario,
  no garantiza costo cero de infraestructura.

## Configuración y secretos

Usar los archivos de ejemplo e instrucciones de cada plataforma. No versionar
`.env`, `*.local.xcconfig`, contraseñas, secretos OAuth, service_role, certificados
ni ajustes personales de firma. El secreto de Google se configura sólo en Supabase.
Las credenciales locales de esta máquina y su firma de Xcode no se publican.

## Verificación de esta versión

- **Lógica:** 9 pruebas Android de acceso/PKCE aprobadas y verificador iOS aprobado,
  incluida la regresión simulada entrar → cerrar sesión → volver a entrar.
- **Compilación:** APK debug Android y build genérico iOS sin firma aprobados.
- **Dispositivos/diseño:** el propietario confirmó el acceso y reingreso con Google
  en iPhone. Falta repetirlo en Android y completar intercambio entre dos cuentas;
  no se afirma homologación visual real sin verla en ambas plataformas.

Los archivos personales de Xcode excluidos no forman parte de esa evidencia de
portabilidad. Cada desarrollador debe seleccionar su propio equipo de firma.
Antes de beta pública quedan la revisión de acceso equivalente exigido por Apple,
SMTP para registro/recuperación por correo y las pruebas físicas pendientes.

Detalles para integración: `AXCIS_ANDROID_ONLINE.md` e
`ios/AXCIS_BACKEND_HANDOFF.md`.
