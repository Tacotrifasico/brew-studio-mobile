import { createClient } from "npm:@supabase/supabase-js@2";

const headers = { "Content-Type": "application/json" };

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return new Response(JSON.stringify({ error: "method_not_allowed" }), { status: 405, headers });
  const authorization = request.headers.get("Authorization");
  if (!authorization) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers });
  const body = await request.json().catch(() => null);
  if (body?.confirmation !== "ELIMINAR") return new Response(JSON.stringify({ error: "confirmation_required" }), { status: 400, headers });

  const url = Deno.env.get("SUPABASE_URL") ?? ""; const anon = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const userClient = createClient(url, anon, { global: { headers: { Authorization: authorization } }, auth: { persistSession: false } });
  const { data: { user }, error: authError } = await userClient.auth.getUser();
  if (authError || !user) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers });

  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!serviceRole) return new Response(JSON.stringify({ error: "server_not_configured" }), { status: 503, headers });
  const admin = createClient(url, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });
  const { error } = await admin.auth.admin.deleteUser(user.id, false);
  if (error) return new Response(JSON.stringify({ error: "account_delete_failed" }), { status: 500, headers });
  return new Response(JSON.stringify({ deleted: true }), { status: 200, headers });
});
