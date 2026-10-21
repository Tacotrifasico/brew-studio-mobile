export const promptVersion = "brew-adjustment-v2";

const scoreKeys = ["aroma", "acidity", "sweetness", "body", "bitterness", "finish"];
const numericKeys = ["coffeeGrams", "waterMl", "ratio", "temperatureC", "grindClicks", "timeSeconds", "extractionIndex", ...scoreKeys];

export function validInput(value) {
  if (!value || Array.isArray(value) || typeof value !== "object") return false;
  const method = typeof value.method === "string" ? value.method.trim() : "";
  if (!method || method.length > 80 || /[\u0000-\u001f\u007f]/u.test(method)) return false;
  if (!numericKeys.every((key) => typeof value[key] === "number" && Number.isFinite(value[key]))) return false;
  return value.coffeeGrams > 0 && value.coffeeGrams <= 100 &&
    Number.isInteger(value.waterMl) && value.waterMl > 0 && value.waterMl <= 2_000 &&
    value.ratio >= 1 && value.ratio <= 40 &&
    Number.isInteger(value.temperatureC) && value.temperatureC >= 0 && value.temperatureC <= 100 &&
    Number.isInteger(value.grindClicks) && value.grindClicks >= 0 && value.grindClicks <= 200 &&
    Number.isInteger(value.timeSeconds) && value.timeSeconds > 0 && value.timeSeconds <= 3_600 &&
    value.extractionIndex >= 0 && value.extractionIndex <= 3 &&
    scoreKeys.every((key) => Number.isInteger(value[key]) && value[key] >= 0 && value[key] <= 100);
}

export function validatedOutput(value) {
  if (typeof value !== "string") return null;
  const text = value.trim();
  if (!text || text.length > 800 || /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/u.test(text)) return null;
  if (text.split(/\s+/u).length > 90) return null;
  return text;
}
