import { createClient } from "npm:@supabase/supabase-js@2";

const jsonHeaders = { "Content-Type": "application/json" };
const promptVersion = "brew-adjustment-v1";

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return new Response(JSON.stringify({ error: "method_not_allowed" }), { status: 405, headers: jsonHeaders });
  const authorization = request.headers.get("Authorization");
  if (!authorization) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: jsonHeaders });

  const supabase = createClient(Deno.env.get("SUPABASE_URL") ?? "", Deno.env.get("SUPABASE_ANON_KEY") ?? "", {
    global: { headers: { Authorization: authorization } }, auth: { persistSession: false },
  });
  const { data: { user }, error: authError } = await supabase.auth.getUser();
  if (authError || !user) return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: jsonHeaders });

  const input = await request.json().catch(() => null);
  if (!validInput(input)) return new Response(JSON.stringify({ error: "invalid_input" }), { status: 400, headers: jsonHeaders });

  const windowStart = new Date(Date.now() - 60_000).toISOString();
  const { count } = await supabase.from("ai_request_log").select("id", { count: "exact", head: true }).gte("created_at", windowStart);
  if ((count ?? 0) >= 5) return new Response(JSON.stringify({ error: "rate_limited" }), { status: 429, headers: jsonHeaders });
  const { error: logError } = await supabase.from("ai_request_log").insert({ prompt_version: promptVersion });
  if (logError) return new Response(JSON.stringify({ error: "rate_limit_unavailable" }), { status: 503, headers: jsonHeaders });

  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) return new Response(JSON.stringify({ error: "ai_unavailable" }), { status: 503, headers: jsonHeaders });
  const model = Deno.env.get("GEMINI_MODEL") ?? "gemini-3.5-flash";
  const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), 15_000);
  try {
    const prompt = `Eres un asistente barista. Interpreta estos resultados calculados localmente; no recalcules ni inventes valores. Sugiere un solo ajuste reversible en español y explica el motivo en máximo 90 palabras. Datos: ${JSON.stringify(input)}`;
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`, {
      method: "POST", signal: controller.signal,
      headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
      body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }], generationConfig: { temperature: 0.3, maxOutputTokens: 180 } }),
    });
    if (!response.ok) return new Response(JSON.stringify({ error: response.status === 429 ? "quota_exhausted" : "ai_unavailable" }), { status: response.status === 429 ? 429 : 502, headers: jsonHeaders });
    const output = await response.json();
    const text = output?.candidates?.[0]?.content?.parts?.map((part: { text?: string }) => part.text ?? "").join("").trim();
    if (!text || text.length > 800) return new Response(JSON.stringify({ error: "invalid_ai_output" }), { status: 502, headers: jsonHeaders });
    return new Response(JSON.stringify({ text, source: "gemini", promptVersion }), { status: 200, headers: jsonHeaders });
  } catch {
    return new Response(JSON.stringify({ error: "ai_timeout" }), { status: 504, headers: jsonHeaders });
  } finally { clearTimeout(timeout); }
});

function validInput(value: Record<string, unknown> | null): boolean {
  if (!value || typeof value.method !== "string" || value.method.length > 80) return false;
  const numericKeys = ["coffeeGrams", "waterMl", "ratio", "temperatureC", "grindClicks", "timeSeconds", "extractionIndex", "aroma", "acidity", "sweetness", "body", "bitterness", "finish"];
  if (!numericKeys.every((key) => typeof value[key] === "number" && Number.isFinite(value[key]))) return false;
  return Number(value.coffeeGrams) > 0 && Number(value.coffeeGrams) <= 100 && Number(value.waterMl) > 0 && Number(value.waterMl) <= 2000 &&
    ["aroma", "acidity", "sweetness", "body", "bitterness", "finish"].every((key) => Number(value[key]) >= 0 && Number(value[key]) <= 100);
}
