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
- `SocialShare`, `ShareLike`, `ShareSave`, `ActivityItem`

Relaciones por UUID estables. Una sesión histórica conserva identificadores y snapshots mínimos de nombres/valores para sobrevivir al borrado lógico de inventario.

`LabExperiment` conserva método, dosis, agua, ratio, temperatura Celsius, clicks, frescura, duración, altitud, ciudad, notas, índice de extracción y resumen calculado. También incluye fechas, versión, estado de sincronización y borrado lógico.

`Grinder` conserva identidad, marca, modelo, tipo manual/eléctrico, unidad de escala, límites, calibración y notas. No se fusiona con `Equipment`.

`Equipment` conserva tipo canónico, nombre, marca, modelo, capacidad, configuración, notas, favorito y estado activo. Las sesiones futuras guardarán UUID y snapshot mínimo para conservar historial tras el borrado lógico.

`Recipe` → `RecipeIngredient` y `RecipeStep` por `recipeId`. La receta conserva tipo, intención, método sugerido, favorito, etiquetas, visibilidad y procedencia de copias. Ingredientes y pasos mantienen orden y UUID independientes.

`Technique` → `TechniqueStep` por `techniqueId`; puede referenciar receta, grano, molino y método sin absorber esas entidades. Los pasos guardan duración, agua agregada y acumulada, gesto, intensidad, cobertura, flujo, acción secundaria y notas.

`BrewSession` registra una ejecución terminada sin fusionarse con la técnica. Conserva referencias opcionales a técnica, receta, café y molino; snapshots de sus nombres, método, dosis, agua, ratio, temperatura, molienda, tiempo ejecutado y la secuencia completa de pasos en JSON. Incluye los mismos metadatos de propiedad, versión, sincronización y borrado lógico que el resto de entidades sincronizables.

`Tasting` guarda familia y notas de sabor, textura, limpieza, persistencia, seis atributos sensoriales, valoración, NPS, notas libres, etapa térmica y vínculo opcional con una preparación. `TastingObservation` conserva cada lectura durante el enfriamiento por `tastingId`. `CupSession` relaciona una cata con una preparación sin impedir catas independientes y mantiene snapshots de técnica y café para proteger el historial.

`UserProfile` usa el UUID de Auth como identidad y conserva nombre, alias, biografía, color de avatar, métodos favoritos y privacidad. Las estadísticas del Hub se derivan de recetas, técnicas, preparaciones y catas activas; no son campos almacenados ni valores simulados.

`syncStatus`: `synced`, `pendingCreate`, `pendingUpdate`, `pendingDelete`, `conflict`, `error`.
