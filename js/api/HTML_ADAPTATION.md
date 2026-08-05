# Guía de Adaptación para la Versión HTML Web de Taller del Brewther

Esta guía contiene la adaptación exacta de las funciones sociales e interacciones del HTML existente para que se comuniquen de forma transparente con el backend compartido en Supabase, importando los módulos JS que creamos en `/js/api/`.

## 1. Importar Módulos en tu HTML Principal

Agrega las etiquetas script al final de tu `body` en tu frontend web HTML:

```html
<!-- Cargar libreria oficial de Supabase primero -->
<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script>

<!-- Cargar entorno -->
<script>
  window.ENV = {
    SUPABASE_URL: "https://TU-PROYECTO.supabase.co",
    SUPABASE_ANON_KEY: "TU-CLAVE-ANONIMA-AQUI"
  };
</script>

<!-- Cargar tus adaptaciones script con type module -->
<script type="module">
  import { authClient } from './js/api/authClient.js';
  import { socialClient } from './js/api/socialClient.js';
  import { syncClient } from './js/api/syncClient.js';

  // Exponer a window para mantener compatibilidad con listeners inline existentes
  window.authClient = authClient;
  window.socialClient = socialClient;
  window.syncClient = syncClient;
  
  // Inicialización automática al cargar
  window.addEventListener('DOMContentLoaded', async () => {
    const { user } = await authClient.getCurrentUser();
    if (user) {
      console.log('Sesión activa detectada para:', user.email);
      // Migración progresiva de datos locales a remotos sin pérdida
      await syncClient.migrateLocalSocialToRemote();
    }
  });
</script>
```

---

## 2. Adaptación de Funciones Sociales del HTML

Reemplaza tus funciones anteriores (que solo guardaban en `localStorage` o hacían mocks) con estas implementaciones reales seguras:

### Función: `shareEntity(entityType, entityId)`

Esta función es llamada cuando el usuario presiona el botón "Compartir" de una receta o técnica:

```javascript
async function shareEntity(entityType, id) {
  // 1. Validar login
  const { user } = await window.authClient.getCurrentUser();
  if (!user) {
    alert("Inicia sesión para poder compartir tus fórmulas en la red del Brew Studio.");
    showLoginDialog(); // Abre la modal de login existente
    return;
  }

  // 2. Preguntar detalles opcionales
  const message = prompt("Escribe una breve descripción o consejo para tu tazas (máx 280 caracteres):", "Probando esta excelente receta en mi taller.");
  if (message === null) return; // Cancelado

  // 3. Ejecutar subida real
  const { share, error } = await window.socialClient.shareEntity(entityType, id, {
    visibility: 'public',
    message: message
  });

  if (error) {
    alert("Error al compartir: " + error.message);
  } else {
    alert(`¡Tu ${entityType === 'recipe' ? 'receta' : 'técnica'} se ha compartido exitosamente en el Feed de Brewther!`);
    loadFeed(); // Refrescar UI del feed
  }
}
```

---

### Función: `importEntity(shareId)`

Esta función es llamada desde el feed público al presionar "Guardar Copia":

```javascript
async function importEntity(shareId) {
  const { user } = await window.authClient.getCurrentUser();
  if (!user) {
    alert("Por favor, inicia sesión para guardar recetas compartidas en tu recetario.");
    return;
  }

  const { copyId, error } = await window.socialClient.importShare(shareId);
  if (error) {
    alert("Error al importar: " + error.message);
  } else {
    alert("Copia importada con éxito. Se ha guardado en tu recetario respetando los créditos del autor original.");
    loadMyRecipesAndTechniques(); // Recargar recetario local
  }
}
```

---

### Función: `forkEntity(shareId)`

Crea una variante editable de la receta o técnica de otro usuario:

```javascript
async function forkEntity(shareId) {
  const { user } = await window.authClient.getCurrentUser();
  if (!user) {
    alert("Inicia sesión para forkear esta fórmula y editarla en tu laboratorio.");
    return;
  }

  const { copyId, error } = await window.socialClient.forkShare(shareId);
  if (error) {
    alert("Error en el fork: " + error.message);
  } else {
    alert("¡Fork creado exitosamente! Ahora puedes editar los parámetros, pasos y tazas libremente en tu pantalla de tazas.");
    loadMyRecipesAndTechniques();
    openFormEditor(copyId); // Abre el editor correspondiente
  }
}
```

---

### Cargar Feed e Inbox en la Interfaz Web HTML

Para pintar las tarjetas en tus secciones correspondientes de Feed e Inbox:

```javascript
async function loadFeed() {
  const feedContainer = document.getElementById('feed-container');
  if (!feedContainer) return;

  feedContainer.innerHTML = '<div class="loader">Cargando café fresco...</div>';

  const { feed, error } = await window.socialClient.getFeed();
  if (error) {
    feedContainer.innerHTML = '<div class="error">Error al cargar feed: ' + error.message + '</div>';
    return;
  }

  if (feed.length === 0) {
    feedContainer.innerHTML = '<div class="empty">Nadie ha publicado café hoy. ¡Sé el primero!</div>';
    return;
  }

  feedContainer.innerHTML = feed.map(item => `
    <div class="card share-card" data-id="${item.id}">
      <div class="card-header">
        <span class="badge ${item.entity_type}-badge">${item.entity_type.toUpperCase()}</span>
        <h3>${item.name}</h3>
        <p class="author">Por: @${item.from_handle || 'anonimo'} (${item.from_name})</p>
      </div>
      <div class="card-body">
        <p class="message">"${item.message || ''}"</p>
        <div class="specs">
          <span>☕ Ratio: 1:${item.payload_snapshot_json.ratio}</span>
          <span>💧 Agua: ${item.payload_snapshot_json.waterMl}ml</span>
          <span>🌡️ Temp: ${item.payload_snapshot_json.temperature || 90}°C</span>
        </div>
      </div>
      <div class="card-footer">
        <button onclick="importEntity('${item.id}')">📥 Registrar Copia</button>
        <button onclick="forkEntity('${item.id}')">🍴 Fork Editable</button>
      </div>
    </div>
  `).join('');
}
```
