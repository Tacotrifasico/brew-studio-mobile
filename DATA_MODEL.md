# Modelo de datos

Todas las entidades sincronizables incluyen `id`, `ownerId`, `createdAt`, `updatedAt`, `version`, `syncStatus` y `deletedAt` cuando corresponda.

Entidades independientes:

- `UserProfile`
- `CoffeeBean`
- `Grinder`
- `Equipment`
- `Recipe`
- `RecipeIngredient`
- `RecipeStep`
- `BrewTechnique`
- `TechniqueStep`
- `BrewSession`
- `Tasting`
- `TastingObservation`
- `CupSession`
- `LabExperiment`
- `SocialShare`, `InboxItem`, `ShareLike`, `ShareSave`, `ActivityItem`

Relaciones por UUID estables. Una sesión histórica conserva identificadores y snapshots mínimos de nombres/valores para sobrevivir al borrado lógico de inventario.

`LabExperiment` conserva referencias opcionales por UUID a método/equipo, receta, técnica, grano y molino; también método, dosis, agua, ratio, temperatura Celsius, clicks, frescura, duración, altitud, ciudad, notas, índice de extracción y resumen calculado. Incluye fechas, versión, estado de sincronización y borrado lógico.

`Grinder` conserva identidad, marca, modelo, tipo manual/eléctrico, unidad de escala, límites, calibración y notas. No se fusiona con `Equipment`. El ajuste concreto vive en `Technique.grindValue/grindDescription/grindUnit` y se copia a `BrewSession`, de modo que el número interno siempre viaja con su descripción histórica.

`CoffeeBean` conserva fechas de tueste y apertura; `CoffeeFreshnessEngine` deriva en tiempo real días, etapa, progreso, recomendación y advertencia de apertura con las reglas de Android. Cerrado/abierto/terminado también se deriva de fecha de apertura y existencias, evitando estados contradictorios. El resultado no se sincroniza porque depende de datos ya sincronizados y, en el caso de frescura, de la fecha actual.

El historial de uso de `CoffeeBean` se deriva por su UUID estable: `BrewSession.beanId` aporta las preparaciones y `CupSession.beanId` las tazas/catas. No existe una relación duplicada ni un contador persistido. El borrado lógico del café no borra esas sesiones; sus snapshots mantienen nombres y parámetros históricos.

`Equipment` conserva tipo canónico, nombre, marca, modelo, capacidad, configuración, notas, favorito y estado activo. Cuando el tipo es `BREWER_METHOD`, activo determina si pertenece al catálogo de la calculadora y favorito si está fijado en sus accesos rápidos. Su UUID viaja por Calculadora, Laboratorio, Preparación y las sesiones históricas; no existe una segunda copia del método.

`Recipe` → `RecipeIngredient` y `RecipeStep` por `recipeId`. La receta conserva tipo, intención, método sugerido, favorito, etiquetas, visibilidad y procedencia de copias. Ingredientes y pasos mantienen orden y UUID independientes.

`Technique` → `TechniqueStep` por `techniqueId`; puede referenciar receta, grano, molino y método sin absorber esas entidades. Los pasos guardan duración, agua agregada y acumulada, gesto, intensidad, cobertura, flujo, acción secundaria y notas.

`BrewSession` registra una ejecución terminada sin fusionarse con la técnica. Conserva referencias opcionales a técnica, receta, método/equipo, café y molino; snapshots de nombres de técnica, receta, método, café y molino, además de dosis, agua, ratio, temperatura, molienda, tiempo ejecutado y la secuencia completa de pasos en JSON. Incluye los mismos metadatos de propiedad, versión, sincronización y borrado lógico que el resto de entidades sincronizables.

`Tasting` guarda familia y notas de sabor, textura, limpieza, persistencia, seis atributos sensoriales, valoración, NPS, notas libres, etapa térmica y vínculo opcional con una preparación. `TastingObservation` conserva cada lectura durante el enfriamiento por `tastingId`. `CupSession` relaciona una cata con una preparación sin impedir catas independientes y congela referencias, nombres, dosis, agua, proporción, temperatura, molienda, duración, estado térmico, valoración, NPS, comentario y fecha. Así, la pestaña Tazas conserva significado aunque cambie o se borre lógicamente el inventario original.

`UserProfile` usa el UUID de Auth como identidad y conserva nombre, alias, biografía, color de avatar, métodos favoritos y privacidad. Las estadísticas del Hub se derivan de recetas, técnicas, preparaciones y catas activas; no son campos almacenados ni valores simulados.

`BrewShare` conserva tipo/UUID de la entidad, autor público, visibilidad, destino opcional, mensaje y snapshot tipado. `InboxItem` enlaza una publicación directa con destinatario, lectura y fecha sin duplicar el snapshot. `ActivityItem` registra acciones sociales reales del usuario. `ShareLike`, `ShareSave`, `BlockedUser` y `ContentReport` son tablas separadas con claves compuestas o unicidad para impedir duplicados. El motivo de reporte conserva una categoría estable y detalles limitados; `BlockedUser` puede eliminarse para desbloquear. Una copia o variante crea un agregado local con UUID nuevo, `IMPORT` o `FORK`, y conserva atribución; nunca adopta el UUID editable del autor. `profiles.is_private` prohíbe crear publicaciones `PUBLIC` tanto en iOS como mediante trigger de base de datos.

`SyncOperation` es una outbox local compactada por tabla y UUID. Conserva operación, payload, intentos, próximo reintento y último error. No se sincroniza a Supabase: coordina el envío de todas las entidades privadas y desaparece lógicamente sólo después de una respuesta remota exitosa.

`syncStatus`: `synced`, `pendingCreate`, `pendingUpdate`, `pendingDelete`, `conflict`, `error`.
