/**
 * Client data synchronizer for migrating offline legacy storage data to Supabase Tables
 * File: /js/api/syncClient.js
 */
import { supabase } from './supabaseClient.js';
import { authClient } from './authClient.js';

export const syncClient = {
  /**
   * Pushes all local recipes to remote Supabase schema if user is logged in
   */
  async syncRecipesToRemote() {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) return { success: false, reason: 'No user active for sync' };

      const { profile } = await authClient.getCurrentProfile(user.id);
      const hostName = profile?.display_name || user.email.split('@')[0];

      // Read local recipes stored in legacy site (typically Key 'local_recipes' or similar)
      const localString = localStorage.getItem('local_recipes') || '[]';
      const localRecipes = JSON.parse(localString);

      if (localRecipes.length === 0) return { success: true, count: 0 };

      let syncCount = 0;
      for (const recipe of localRecipes) {
        if (recipe.remote_synced) continue; // Skip already synced items

        // Map local fields to matching Supabase columns
        const remoteItem = {
          user_id: user.id,
          owner_user_id: user.id,
          owner_display_name: hostName,
          name: (recipe.name || 'Fórmula Local').substring(0, 120),
          method: recipe.method,
          coffee_grams: recipe.coffeeGrams || recipe.coffee_grams,
          water_ml: recipe.waterMl || recipe.water_ml,
          ratio: recipe.ratio,
          temperature: recipe.temperature,
          clicks: recipe.clicks?.toString(),
          notes: (recipe.notes || '').substring(0, 2000),
          visibility: recipe.visibility || 'private',
          original_author_user_id: user.id,
          original_author_name: hostName,
          copy_mode: 'original'
        };

        const { data, error } = await supabase
          .from('recipes')
          .insert(remoteItem)
          .select()
          .single();

        if (!error && data) {
          recipe.remote_synced = true;
          recipe.remote_id = data.id;
          syncCount++;
        }
      }

      // Save updated offline list back as local cache
      localStorage.setItem('local_recipes', JSON.stringify(localRecipes));
      return { success: true, count: syncCount };
    } catch (e) {
      console.error('syncRecipesToRemote failed:', e);
      return { success: false, error: e };
    }
  },

  /**
   * Sychronizes custom techniques to the cloud
   */
  async syncTechniquesToRemote() {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) return { success: false };

      const { profile } = await authClient.getCurrentProfile(user.id);
      const hostName = profile?.display_name || user.email.split('@')[0];

      const localString = localStorage.getItem('local_techniques') || '[]';
      const localTechs = JSON.parse(localString);

      if (localTechs.length === 0) return { success: true, count: 0 };

      let syncCount = 0;
      for (const tech of localTechs) {
        if (tech.remote_synced) continue;

        const remoteItem = {
          user_id: user.id,
          owner_user_id: user.id,
          owner_display_name: hostName,
          name: (tech.name || 'Técnica Local').substring(0, 120),
          method: tech.method,
          coffee_grams: tech.coffeeGrams || tech.coffee_grams,
          water_ml: tech.waterMl || tech.water_ml,
          ratio: tech.ratio,
          temperature: tech.temperatureSuggested || tech.temperature,
          grind_clicks: tech.clicksSugeridos?.toString() || tech.grind_clicks?.toString(),
          notes: (tech.notes || '').substring(0, 2000),
          visibility: tech.visibility || 'private'
        };

        // Insert technique header
        const { data: remoteTech, error: techErr } = await supabase
          .from('techniques')
          .insert(remoteItem)
          .select()
          .single();

        if (!techErr && remoteTech) {
          // If technique has nested steps (typically technique.steps or loaded from subkey)
          const localSteps = tech.steps || [];
          if (localSteps.length > 0) {
            const mappedSteps = localSteps.map((step, idx) => ({
              technique_id: remoteTech.id,
              user_id: user.id,
              step_order: step.stepNumber || step.step_order || idx + 1,
              title: step.title || `Paso ${idx + 1}`,
              duration_sec: step.durationSeconds || step.duration_sec || 0,
              water_add_ml: step.waterAddedMl || step.water_add_ml || 0,
              target_water_ml: step.waterAccumulatedMl || step.target_water_ml || 0,
              gesture: step.gesture || 'tap',
              intensity: step.intensity || 'media',
              note: step.stepNote || step.note || ''
            }));

            await supabase.from('technique_steps').insert(mappedSteps);
          }

          tech.remote_synced = true;
          tech.remote_id = remoteTech.id;
          syncCount++;
        }
      }

      localStorage.setItem('local_techniques', JSON.stringify(localTechs));
      return { success: true, count: syncCount };
    } catch (e) {
      console.error('syncTechniquesToRemote failed:', e);
      return { success: false, error: e };
    }
  },

  /**
   * Core high-level coordinator to sync entire offline profile data on initial login
   */
  async migrateLocalSocialToRemote() {
    const recipesRes = await this.syncRecipesToRemote();
    const techRes = await this.syncTechniquesToRemote();
    
    // Log migration activity
    const { user } = await authClient.getCurrentUser();
    if (user && (recipesRes.count > 0 || techRes.count > 0)) {
      await supabase.from('activity_log').insert({
        user_id: user.id,
        action: 'migrate_legacy_data',
        note: `Migración de datos completada: ${recipesRes.count || 0} recetas, ${techRes.count || 0} técnicas sincronizadas.`
      });
    }

    return {
      recipes: recipesRes,
      techniques: techRes
    };
  }
};
