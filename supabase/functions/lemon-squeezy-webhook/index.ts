import { serve } from "https://deno.land/std@0.192.0/http/server.ts"

const LEMON_SQUEEZY_SECRET = (Deno.env.get("LEMON_SQUEEZY_WEBHOOK_SECRET") ?? "").replace(/^["']|["']$/g, "").trim();
const PRIVATE_KEY_PEM = (Deno.env.get("ECDSA_PRIVATE_KEY_PEM") ?? "").replace(/^["']|["']$/g, "").trim();
const RESEND_API_KEY = (Deno.env.get("RESEND_API_KEY") ?? "").replace(/^["']|["']$/g, "").trim();

// Helper: Convert Hex String to Uint8Array
function hexToBytes(hex: string): Uint8Array {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

// Helper: Import ECDSA Private Key (Supports both JWK JSON and PKCS#8 PEM/Base64)
async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const cleanPem = pem.replace(/^["']|["']$/g, "").trim();

  // If it's a JWK JSON string
  if (cleanPem.startsWith("{")) {
    const jwk = JSON.parse(cleanPem);
    return await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "ECDSA", namedCurve: "P-256" },
      false,
      ["sign"]
    );
  }

  // Fallback to PKCS#8 PEM
  const pemHeader = "-----BEGIN PRIVATE KEY-----";
  const pemFooter = "-----END PRIVATE KEY-----";
  const pemContents = cleanPem
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
  const urlObj = new URL(req.url);
  
  // Debug/diagnostic mode (GET request)
  if (req.method === "GET" || urlObj.searchParams.get("debug") === "true") {
    const diagnostics: Record<string, any> = {
      timestamp: new Date().toISOString(),
      LEMON_SQUEEZY_SECRET_len: LEMON_SQUEEZY_SECRET.length,
      RESEND_API_KEY_len: RESEND_API_KEY.length,
      PRIVATE_KEY_PEM: {
        raw_len: (Deno.env.get("ECDSA_PRIVATE_KEY_PEM") ?? "").length,
        processed_len: PRIVATE_KEY_PEM.length,
        starts_with: PRIVATE_KEY_PEM.substring(0, Math.min(10, PRIVATE_KEY_PEM.length)),
        ends_with: PRIVATE_KEY_PEM.substring(Math.max(0, PRIVATE_KEY_PEM.length - 10)),
      }
    };

    try {
      // 1. Try importing private key
      const privateKey = await importPrivateKey(PRIVATE_KEY_PEM);
      diagnostics.key_import = {
        success: true,
        algorithm: privateKey.algorithm,
        usages: privateKey.usages,
        type: privateKey.type
      };

      // 2. Try simulating signing & encoding
      try {
        const mockPayload = {
          email: "test-buyer@alhaq.uk",
          type: "premium",
          expires: Date.now() + 365 * 24 * 60 * 60 * 1000,
          version: 1
        };
        const mockPayloadJson = JSON.stringify(mockPayload);
        const encoder = new TextEncoder();
        const mockPayloadBytes = encoder.encode(mockPayloadJson);

        const signatureBytes = await crypto.subtle.sign(
          { name: "ECDSA", hash: { name: "SHA-256" } },
          privateKey,
          mockPayloadBytes
        );

        const base64Payload = btoa(mockPayloadJson);
        const base64Signature = btoa(String.fromCharCode(...new Uint8Array(signatureBytes)));
        const licenseKey = `${base64Payload}.${base64Signature}`;

        diagnostics.signing_simulation = {
          success: true,
          licenseKey_len: licenseKey.length,
          licenseKey_preview: licenseKey.substring(0, 20) + "..."
        };
      } catch (signErr) {
        diagnostics.signing_simulation = {
          success: false,
          error_name: signErr.name,
          error_message: signErr.message,
          stack: signErr.stack
        };
      }

    } catch (err) {
      diagnostics.key_import = {
        success: false,
        error_name: err.name,
        error_message: err.message,
        stack: err.stack
      };
    }

    return new Response(JSON.stringify(diagnostics, null, 2), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  }

  if (req.method !== "POST") {
    return new Response("Method Not Allowed", { status: 405 });
  }

  try {
    const rawBody = await req.text();
    
    // 1. Authenticate incoming webhook
    const signature = req.headers.get("x-signature");
    if (!signature) {
      return new Response("Missing Signature", { status: 401 });
    }

    const encoder = new TextEncoder();
    const hmacKey = await crypto.subtle.importKey(
      "raw",
      encoder.encode(LEMON_SQUEEZY_SECRET),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["verify"]
    );

    const verified = await crypto.subtle.verify(
      "HMAC",
      hmacKey,
      hexToBytes(signature),
      encoder.encode(rawBody)
    );

    if (!verified) {
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
          from: "AmnShield Team <noreply@alhaq.uk>",
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
