/**
 * Auth client wrapper using Supabase Auth and Web Profile integrations.
 * File: /js/api/authClient.js
 */
import { supabase } from './supabaseClient.js';

export const authClient = {
  /**
   * Registers a new user, automatically creating their associated public.profiles record
   */
  async signUp(email, password, profileData = {}) {
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: {
          data: {
            display_name: profileData.displayName || email.split('@')[0],
            handle: profileData.handle || (email.split('@')[0] + '_' + Math.floor(1000 + Math.random() * 9000))
          }
        }
      });
      if (error) throw error;
      
      // If there is profile metadata to write immediately
      if (data.user && Object.keys(profileData).length > 0) {
        await this.updateProfile({
          id: data.user.id,
          display_name: profileData.displayName || email.split('@')[0],
          handle: profileData.handle || (email.split('@')[0] + '_' + Math.floor(1000 + Math.random() * 9000)),
          avatar_url: profileData.avatarUrl || null,
          avatar_color: profileData.avatarColor || '#3F7A63'
        });
      }
      return { user: data.user, error: null };
    } catch (e) {
      console.error('Registration failed:', e.message);
      return { user: null, error: e };
    }
  },

  /**
   * Log into the backend and synchronize auth status
   */
  async signIn(email, password) {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password
      });
      if (error) throw error;

      // Sync user profile to localStorage for immediate offline verification
      if (data.user) {
        const { profile } = await this.getCurrentProfile(data.user.id);
        if (profile) {
          localStorage.setItem('brew_studio_active_profile', JSON.stringify(profile));
        }
      }

      return { user: data.user, error: null };
    } catch (e) {
      console.error('Authentication login failed:', e.message);
      return { user: null, error: e };
    }
  },

  /**
   * Core logout operation
   */
  async signOut() {
    try {
      await supabase.auth.signOut();
      localStorage.removeItem('brew_studio_active_profile');
      return { error: null };
    } catch (e) {
      return { error: e };
    }
  },

  /**
   * Retrieves active authenticated user session
   */
  async getCurrentUser() {
    try {
      const { data: { user }, error } = await supabase.auth.getUser();
      if (error) throw error;
      return { user };
    } catch (e) {
      return { user: null };
    }
  },

  /**
   * Fetch user profiles details
   */
  async getCurrentProfile(userId = null) {
    try {
      let uid = userId;
      if (!uid) {
        const { user } = await this.getCurrentUser();
        if (!user) return { profile: null };
        uid = user.id;
      }

      const { data, error } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', uid)
        .single();

      if (error) throw error;
      return { profile: data, error: null };
    } catch (e) {
      // Fallback local cache check
      const cached = localStorage.getItem('brew_studio_active_profile');
      if (cached) {
        return { profile: JSON.parse(cached), error: null };
      }
      return { profile: null, error: e };
    }
  },

  /**
   * Updates display profiles details safely
   */
  async updateProfile(profileUpdates) {
    try {
      const { user } = await this.getCurrentUser();
      if (!user) throw new Error('Not authenticated');

      const cleanUpdates = {
        id: user.id,
        email: user.email,
        display_name: profileUpdates.display_name?.substring(0, 80),
        handle: profileUpdates.handle?.substring(0, 32).toLowerCase().replace(/[^a-z0-9_]/g, ''),
        avatar_url: profileUpdates.avatar_url,
        avatar_color: profileUpdates.avatar_color || '#3F7A63',
        updated_at: new Date().toISOString()
      };

      const { data, error } = await supabase
        .from('profiles')
        .upsert(cleanUpdates)
        .select()
        .single();

      if (error) throw error;
      
      localStorage.setItem('brew_studio_active_profile', JSON.stringify(data));
      return { profile: data, error: null };
    } catch (e) {
      return { profile: null, error: e };
    }
  }
};
