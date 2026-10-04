# BARC reversible · Android / iOS

## Uso

Tocar **Calculadora barista** gira la tarjeta. El reverso muestra el método del
frente (no permite cambiarlo), Grano, Clics de molino y Temperatura. Tocar el título
otra vez vuelve al cálculo. Cambiar los controles guarda automáticamente; se muestra
un error si no se guardó. No cambia cantidades ni favoritos del frente.

Android: Almacén → Café → editar el grano → Ajustes por método.
iOS: Almacén → Café → abrir el grano → Cómo lo preparo.
Elegir el método ahí y editar los mismos ajustes. Al volver al Taller, seleccionar
el grano recupera su combinación. V60 y AeroPress conservan valores independientes.
Preparar y Laboratorio reciben el grano, la temperatura y los clics. Una preparación
en curso no debe ser reemplazada; primero hay que cancelarla.

## Persistencia local

Android: columna Bean.brewProfilesJSON, Room 9 → 10, sin borrar inventario.
iOS: CoffeeBeanRecord.brewProfilesJSON; migración inferida explícita del modelo
programático anterior. La selección de grano se conserva por cuenta en preferencias.
Las notas del café no se usan para almacenar configuraciones.

JSON compartido:
```json
{
  "v60": {"methodName":"V60","clicks":22,"temperatureC":91},
  "aeropress": {"methodName":"AeroPress","clicks":15,"temperatureC":87}
}
```

Clave: nombre de método sin espacios perimetrales, sin diacríticos, en minúsculas
con locale estable. Se admite un método personalizado. Celsius es la unidad
persistida; Fahrenheit sólo es presentación. Límites: 1–200 clics, 1–100 °C.
Un método sin asociación no hereda los ajustes de otro método/grano.
Los clics representan el molino que usa la persona, no una equivalencia universal.

## AXCIS-ONLINE: pendiente, no presentar como sincronizado

Estos perfiles son locales por ahora. Android todavía no sincroniza el inventario;
el descriptor remoto iOS existente omite este campo y no borra la copia local.

Antes de habilitar sincronización entre teléfonos:

1. Añadir a public.beans una columna brew_profiles JSONB NOT NULL DEFAULT '{}',
   con CHECK (jsonb_typeof(brew_profiles) = 'object'). Conservar RLS por propietario.
2. Mapear Bean.brewProfilesJSON (Android) y CoffeeBeanRecord.brewProfilesJSON (iOS)
   a brew_profiles como objeto JSON, no como cadena JSON ni campo notes.
3. Preservar la copia local cuando un servidor viejo omita el campo; no reemplazar
   el mapa con '{}' accidentalmente. Resolver conflictos por versión del grano
   o implementar merge por método, nunca sobrescribir otro método por silencio.
4. Conectar push/pull de beans en Android y añadir el campo al descriptor iOS
   sólo después de desplegar la migración. Un fallo remoto no borra ajustes locales.
5. Probar Android → iPhone y viceversa, offline/reconexión, dos métodos del mismo
   grano, dos granos, cambio de cuenta y conflictos concurrentes.

No se ha desplegado ninguna migración remota como parte de este cambio.

## Verificación

BeanBrewProfilesTest (Android): mapa por método, migración preservando datos,
persistencia Room, selección recuperada y transferencia a Lab/Preparar.
LabGoldenVerifier (iOS): store SQLite real del modelo anterior → modelo nuevo,
reapertura, selección persistente y transferencia a Lab/Preparar.
La compilación y las pruebas de lógica no sustituyen la prueba visual en teléfonos.

