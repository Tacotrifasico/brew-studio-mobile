# Objetivo del proyecto

Entregar Brew Studio (Cupa) para iOS como aplicación nativa estable, homologada funcionalmente con la referencia Android/Google AI Studio y preparada técnicamente para TestFlight y App Store.

La homologación exige:

1. Datos equivalentes, persistencia offline, propiedad por usuario y sincronización segura.
2. Fórmulas, reglas, CRUD, temporizadores y flujos completos equivalentes.
3. Capacidades y jerarquía visual equivalentes, adaptadas a patrones nativos de iOS.

El proyecto no se considera terminado porque compile o muestre pantallas. La definición de terminado está en `RELEASE_CHECKLIST.md` y requiere paridad validada, pruebas críticas aprobadas, build Release y Archive.

## Fuentes de verdad

1. Rama `main` de `https://github.com/Tacotrifasico/brew-studio-mobile`.
2. Implementación Android vigente y su esquema Supabase.
3. Proyecto iOS de este directorio.
4. Documentos de decisiones y paridad.

Última referencia Android auditada: commit `392fd2bbb3c4b906d30af1799ca15612eafa34bb`, integrado y confirmado contra `origin/main` el 5 de septiembre de 2026. Incluye la continuidad Calculadora → Preparar y la selección persistente del favorito.
