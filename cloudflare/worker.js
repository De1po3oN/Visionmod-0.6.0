// Vision Cosmetics backend for Cloudflare Workers + Workers KV.
// Bind a namespace as COSMETICS_KV. No admin token is embedded in the mod.
// SECURITY NOTE: first-write UUID claiming is NOT proof of Mojang account ownership.

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const ID = /^(none|[a-z0-9_-]{1,32})$/;
const SECRET = /^[a-f0-9]{64}$/;

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff"
    }
  });
}

function validateModel(model) {
  if (model === null) return true;
  if (!model || Array.isArray(model) || typeof model !== "object") return false;
  if (!Array.isArray(model.boxes) || model.boxes.length < 1 || model.boxes.length > 64) return false;
  for (const box of model.boxes) {
    if (!box || typeof box !== "object" || Array.isArray(box)) return false;
    if (!Array.isArray(box.from) || !Array.isArray(box.to) ||
        box.from.length !== 3 || box.to.length !== 3) return false;
    for (const n of [...box.from, ...box.to]) {
      if (typeof n !== "number" || !Number.isFinite(n) || Math.abs(n) > 3) return false;
    }
    if (typeof box.color !== "string" || !/^#[0-9a-fA-F]{6}$/.test(box.color)) return false;
  }
  return true;
}

function validateSelection(c) {
  if (!c || typeof c !== "object" || Array.isArray(c)) return false;
  if (!ID.test(c.head) || !ID.test(c.back)) return false;
  if (!Object.hasOwn(c, "headModel") || !Object.hasOwn(c, "backModel")) return false;
  if (!validateModel(c.headModel) || !validateModel(c.backModel)) return false;
  const allowed = new Set(["head", "back", "headModel", "backModel"]);
  return Object.keys(c).every(k => allowed.has(k));
}

async function sha256(s) {
  const bytes = new TextEncoder().encode(s);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest), x => x.toString(16).padStart(2, "0")).join("");
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/health")) {
      return json({ status: "ok", service: "vision-cosmetics", version: "0.4.1",
        kv: env.COSMETICS_KV ? "configured" : "missing" });
    }
    if (request.method === "GET" && url.pathname === "/kv-test") {
      if (!env.COSMETICS_KV) return json({ status: "error", kv: "missing" }, 503);
      try {
        await env.COSMETICS_KV.get("vision_test");
        return json({ status: "ok", kv: "connected" });
      } catch {
        return json({ status: "error", kv: "unavailable" }, 503);
      }
    }

    const match = /^\/cosmetics\/([^/]+)$/.exec(url.pathname);
    if (!match || (request.method !== "GET" && request.method !== "PUT")) {
      return json({ error: "Endpoint not found" }, 404);
    }
    const uuid = match[1].toLowerCase();
    if (!UUID.test(uuid)) return json({ error: "Invalid UUID" }, 400);
    if (!env.COSMETICS_KV) return json({ error: "KV binding missing" }, 503);
    const key = "player:" + uuid;

    if (request.method === "GET") {
      try {
        const stored = await env.COSMETICS_KV.get(key, "json");
        const c = stored?.cosmetics || { head: null, back: null, headModel: null, backModel: null };
        return json({ uuid, cosmetics: c });
      } catch {
        return json({ error: "KV read failed" }, 503);
      }
    }

    // PUT: per-installation private token; never sent to any other Minecraft clients.
    const authorization = request.headers.get("Authorization") || "";
    const token = authorization.startsWith("Bearer ") ? authorization.slice(7) : "";
    if (!SECRET.test(token)) return json({ error: "Missing or invalid authorization" }, 401);
    const contentLength = Number(request.headers.get("content-length") || 0);
    if (contentLength > 12_288) return json({ error: "Payload too large" }, 413);
    let selection;
    try {
      const text = await request.text();
      if (text.length > 12_288) return json({ error: "Payload too large" }, 413);
      selection = JSON.parse(text);
    } catch {
      return json({ error: "Invalid JSON" }, 400);
    }
    if (!validateSelection(selection)) return json({ error: "Invalid cosmetic model or selection" }, 400);

    try {
      const old = await env.COSMETICS_KV.get(key, "json");
      const tokenHash = await sha256(token);
      if (old?.ownerHash && old.ownerHash !== tokenHash) {
        return json({ error: "UUID already claimed by another installation" }, 403);
      }
      // Upgrade a legacy record that has no owner key on the first write.
      // Like every first claim, this does not verify account ownership.
      await env.COSMETICS_KV.put(key, JSON.stringify({
        ownerHash: tokenHash,
        cosmetics: selection,
        updatedAt: new Date().toISOString()
      }));
      return json({ status: "saved", uuid, cosmetics: selection });
    } catch {
      return json({ error: "KV write failed" }, 503);
    }
  }
};
