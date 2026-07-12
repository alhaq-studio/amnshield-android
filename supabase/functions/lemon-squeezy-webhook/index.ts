import { serve } from "https://deno.land/std@0.192.0/http/server.ts"

const LEMON_SQUEEZY_SECRET = Deno.env.get("LEMON_SQUEEZY_WEBHOOK_SECRET") ?? "";
const PRIVATE_KEY_PEM = Deno.env.get("ECDSA_PRIVATE_KEY_PEM") ?? "";
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY") ?? "";

// Helper: Verify Lemon Squeezy Webhook Signature
async function verifyLemonSqueezySignature(req: Request, rawBody: string): Promise<boolean> {
  const signature = req.headers.get("x-signature");
  if (!signature) return false;

  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(LEMON_SQUEEZY_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["verify"]
  );

  const verified = await crypto.subtle.verify(
    "HMAC",
    key,
    hexToBytes(signature),
    encoder.encode(rawBody)
  );

  return verified;
}

// Helper: Convert Hex String to Uint8Array
function hexToBytes(hex: string): Uint8Array {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

// Helper: Import ECDSA Private Key from PEM
async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const pemHeader = "-----BEGIN PRIVATE KEY-----";
  const pemFooter = "-----END PRIVATE KEY-----";
  const pemContents = pem
    .replace(pemHeader, "")
    .replace(pemFooter, "")
    .replace(/\s+/g, "");
  
  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));

  return await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"]
  );
}

// Main Webhook Handler
Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method Not Allowed", { status: 405 });
  }

  try {
    const rawBody = await req.text();
    
    // 1. Authenticate incoming webhook
    const isValid = await verifyLemonSqueezySignature(req, rawBody);
    if (!isValid) {
      return new Response("Unauthorized Signature", { status: 401 });
    }

    const event = JSON.parse(rawBody);
    const eventName = event.meta.event_name;

    // 2. Filter for successful orders
    if (eventName === "order_created") {
      const attributes = event.data.attributes;
      const email = attributes.user_email;
      const variantId = attributes.variant_id; // Use this to determine license type if you have tiers

      // Calculate expiry (e.g., 1 year from now)
      const expiryTimestamp = Date.now() + 365 * 24 * 60 * 60 * 1000;

      // 3. Construct Payload (With Versioning Pre-baked!)
      const payload = {
        email: email,
        type: "premium",
        expires: expiryTimestamp,
        version: 1 // Ready for future key rotation
      };

      const payloadJson = JSON.stringify(payload);
      const encoder = new TextEncoder();
      const payloadBytes = encoder.encode(payloadJson);

      // 4. Sign the Payload using ECDSA
      const privateKey = await importPrivateKey(PRIVATE_KEY_PEM);
      const signatureBytes = await crypto.subtle.sign(
        { name: "ECDSA", hash: { name: "SHA-256" } },
        privateKey,
        payloadBytes
      );

      // 5. Format License Key: Base64(Payload).Base64(Signature)
      const base64Payload = btoa(payloadJson);
      const base64Signature = btoa(String.fromCharCode(...new Uint8Array(signatureBytes)));
      const licenseKey = `${base64Payload}.${base64Signature}`;

      // 6. Deliver License via Resend Email API
      await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${RESEND_API_KEY}`,
        },
        body: JSON.stringify({
          from: "AmnShield Team <noreply@yourdomain.com>",
          to: [email],
          subject: "Your AmnShield Premium License Key",
          html: `
            <p>Thank you for purchasing AmnShield Premium!</p>
            <p>Here is your license key. Copy and paste this directly into the app's Profile settings:</p>
            <pre style="background: #f4f4f4; padding: 15px; border-radius: 5px; word-break: break-all;">${licenseKey}</pre>
            <p>This key is valid until: <b>${new Date(expiryTimestamp).toLocaleDateString()}</b></p>
          `,
        }),
      });

      return new Response(JSON.stringify({ success: true, message: "License generated and emailed." }), { status: 200 });
    }

    return new Response("Event ignored", { status: 200 });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  }
});
