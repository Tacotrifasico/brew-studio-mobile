/**
 * Supabase Web API Client Initializer
 * File: /js/api/supabaseClient.js
 */

// Reads environment variables from global runtime configuration or fallback declarations
const SUPABASE_URL = window.ENV?.SUPABASE_URL || 'https://your-supabase-url.supabase.co';
const SUPABASE_ANON_KEY = window.ENV?.SUPABASE_ANON_KEY || 'your-anon-key-here';

// Initialize core client using the standard imported @supabase/supabase-js library
export const supabase = window.supabase 
  ? window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY)
  : {
      auth: {
        signUp: async () => ({ error: { message: 'Supabase library is not loaded on window' } }),
        signInWithPassword: async () => ({ error: { message: 'Supabase library is not loaded on window' } }),
        signOut: async () => ({ error: null }),
        getUser: async () => ({ data: { user: null } })
      }
    };
