/**
 * Web Social API integration client (Feed, Shares, Likes, Saves, Imports, Forks)
 * File: /js/api/socialClient.js
 */
import { supabase } from './supabaseClient.js';
import { authClient } from './authClient.js';

export const socialClient = {
  /**
   * Retrieves all public shared entities (Recipes/Techniques) for the global activity feed
   */
  async getFeed() {
    try {
      const { data, error } = await supabase
        .from('shares')
        .select(`
          *,
          likes_count:share_likes(count),
          saves_count:share_saves(count)
        `)
        .eq('visibility', 'public')
        .order('created_at', { ascending: false });

      if (error) throw error;
      return { feed: data || [], error: null };
    } catch (e) {
      console.error('getFeed failed:', e.message);
      return { feed: [], error: e };
    }
  },

  /**
   * Retrieves personal direct transfers/inbox messages directed specifically to this user
   */
  async getInbox() {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) return { inbox: [], error: new Error('Usuario no autenticado') };

      const { data, error } = await supabase
        .from('inbox_items')
        .select(`
          id,
          read_at,
          created_at,
          share:shares(*)
        `)
        .eq('target_user_id', user.id)
        .order('created_at', { ascending: false });

      if (error) throw error;
      return { inbox: data || [], error: null };
    } catch (e) {
      return { inbox: [], error: e };
    }
  },

  /**
   * Share a local, existing recipe or technique with details
   */
  async shareEntity(entityType, entityId, options = {}) {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) throw new Error('Usuario no autenticado. Inicie sesion para compartir.');

      const { profile } = await authClient.getCurrentProfile(user.id);
      const hostName = profile?.display_name || user.email.split('@')[0];
      const hostHandle = profile?.handle || '';

      // 1. Fetch raw entity data from correct private table
      const tableName = entityType === 'recipe' ? 'recipes' : 'techniques';
      const { data: entityData, error: fetchErr } = await supabase
        .from(tableName)
        .select('*')
        .eq('id', entityId)
        .single();

      if (fetchErr || !entityData) {
        throw new Error(`No se encontro la entidad para compartir: ${fetchErr?.message}`);
      }

      // If it is a technique, join steps
      let payloadSnapshot = { ...entityData };
      if (entityType === 'technique') {
        const { data: steps } = await supabase
          .from('technique_steps')
          .select('*')
          .eq('technique_id', entityId)
          .order('step_order', { ascending: true });
        payloadSnapshot.steps = steps || [];
      }

      // 2. Build share payload
      const sharePayload = {
        entity_type: entityType,
        entity_id: entityId,
        from_user_id: user.id,
        from_name: hostName,
        from_handle: hostHandle,
        target_user_id: options.targetUserId || null,
        visibility: options.visibility || 'public',
        name: entityData.name,
        subtitle: entityType === 'recipe' ? `Fórmula de ${entityData.method}` : `Rutina para ${entityData.method}`,
        message: (options.message || '').substring(0, 280),
        payload_snapshot_json: payloadSnapshot,
        original_author_user_id: entityData.original_author_user_id || user.id,
        original_author_name: entityData.original_author_name || hostName,
        original_entity_id: entityData.original_entity_id || entityId
      };

      const { data: shareResult, error: shareErr } = await supabase
        .from('shares')
        .insert(sharePayload)
        .select()
        .single();

      if (shareErr) throw shareErr;

      // Update original item is_shared tag to true
      await supabase
        .from(tableName)
        .update({ is_shared: true, visibility: options.visibility || 'public' })
        .eq('id', entityId);

      // 3. If direct message, route to direct target_user_id inbox
      if (options.targetUserId && options.visibility === 'direct') {
        await supabase.from('inbox_items').insert({
          share_id: shareResult.id,
          target_user_id: options.targetUserId
        });
      }

      // Log behavior in actions timeline
      await supabase.from('activity_log').insert({
        user_id: user.id,
        action: 'share_' + entityType,
        entity_type: entityType,
        entity_id: entityId,
        share_id: shareResult.id,
        note: `Compartió ${entityType === 'recipe' ? 'receta' : 'técnica'} '${entityData.name}'`
      });

      return { share: shareResult, error: null };
    } catch (e) {
      console.error('shareEntity failed:', e.message);
      return { share: null, error: e };
    }
  },

  /**
   * Express support for shared card
   */
  async likeShare(shareId) {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) throw new Error('Not authenticated');

      const { data, error } = await supabase
        .from('share_likes')
        .insert({ share_id: shareId, user_id: user.id })
        .select()
        .single();

      if (error) throw error;
      return { like: data, error: null };
    } catch (e) {
      return { like: null, error: e };
    }
  },

  /**
   * Revoke support
   */
  async unlikeShare(shareId) {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) throw new Error('Not authenticated');

      const { error } = await supabase
        .from('share_likes')
        .delete()
        .eq('share_id', shareId)
        .eq('user_id', user.id);

      if (error) throw error;
      return { success: true, error: null };
    } catch (e) {
      return { success: false, error: e };
    }
  },

  /**
   * Save share to profile bookmarks
   */
  async saveShare(shareId) {
    try {
      const { user } = await authClient.getCurrentUser();
      if (!user) throw new Error('Not authenticated');

      const { data, error } = await supabase
        .from('share_saves')
        .insert({ share_id: shareId, user_id: user.id })
        .select()
        .single();

      if (error) throw error;
      return { save: data, error: null };
    } catch (e) {
      return { save: null, error: e };
    }
  },

  /**
   * RPC Trigger: Import unmodified items keeping full initial authorship
   */
  async importShare(shareId) {
    try {
      const { data: share } = await supabase
        .from('shares')
        .select('entity_type')
        .eq('id', shareId)
        .single();
      
      if (!share) throw new Error('Compartido no válido');
      
      const rpcName = share.entity_type === 'recipe' 
        ? 'import_share_as_recipe' 
        : 'import_share_as_technique';

      const { data: copyId, error } = await supabase.rpc(rpcName, { input_share_id: shareId });
      
      if (error) throw error;
      return { copyId, error: null };
    } catch (e) {
      return { copyId: null, error: e };
    }
  },

  /**
   * RPC Trigger: Fork items making user current entity creator while maintaining author attribution
   */
  async forkShare(shareId) {
    try {
      const { data: share } = await supabase
        .from('shares')
        .select('entity_type')
        .eq('id', shareId)
        .single();
      
      if (!share) throw new Error('Compartido no válido');
      
      const rpcName = share.entity_type === 'recipe' 
        ? 'fork_share_as_recipe' 
        : 'fork_share_as_technique';

      const { data: copyId, error } = await supabase.rpc(rpcName, { input_share_id: shareId });
      
      if (error) throw error;
      return { copyId, error: null };
    } catch (e) {
      return { copyId: null, error: e };
    }
  }
};
