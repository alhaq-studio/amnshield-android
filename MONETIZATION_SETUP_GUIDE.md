# Monetization Setup Guide: Lemon Squeezy + Supabase

This guide walks you through setting up your **Lemon Squeezy** merchant account, configuring the webhook, and deploying the webhook listener function to **Supabase**.

---

## 🛠️ Step 1: Set Up Lemon Squeezy

1. **Sign Up/Log In:**
   Go to [lemonsqueezy.com](https://www.lemonsqueezy.com/) and set up your store.

2. **Create a Product:**
   * Go to **Products** and click **Add New Product**.
   * Name: `AmnShield Premium`
   * Pricing Model: Choose **Single Payment** (e.g., Lifetime License) or **Subscription** (e.g., Yearly).
   * Save the product. Copy the **Variant ID** (visible under the product details/variant list) — you will need this if you want to partition premium tiers.

3. **Configure Webhook Settings:**
   * Go to **Settings > Webhooks** in your Lemon Squeezy dashboard.
   * Click **Add Webhook**.
   * **Callback URL:** Enter your Supabase Edge Function endpoint:
     `https://<your-supabase-project-id>.supabase.co/functions/v1/lemon-squeezy-webhook`
   * **Signing Secret:** Enter a strong, random password/token (e.g., a 32-character string). Keep this safe, you will save this as a Supabase environment secret.
   * **Events to Send:** Check the box for **`order_created`**.
   * Click **Save Webhook**.

---

## 🚀 Step 2: Set Up and Deploy to Supabase

1. **Install the Supabase CLI:**
   Ensure you have the Supabase CLI installed on your system:
   ```bash
   npm install -g supabase
   ```

2. **Initialize and Login:**
   Log into your Supabase account and link your local workspace to your project:
   ```bash
   supabase login
   # Go to your AmnShield project directory
   supabase link --project-ref your-supabase-project-id
   ```

3. **Configure Environment Secrets:**
   You must set three environment secrets in your Supabase project so the Deno Edge Function can access them:
   ```bash
   # 1. The signing secret you set in Lemon Squeezy Webhook settings:
   supabase secrets set LEMON_SQUEEZY_WEBHOOK_SECRET="your_webhook_signing_secret"

    # 2. Your ECDSA PKCS#8 Private Key (from license_keys.md):
    supabase secrets set ECDSA_PRIVATE_KEY_PEM="your_pkcs8_private_key_pem_here"

   # 3. Your Resend API Key (for sending emails):
   supabase secrets set RESEND_API_KEY="re_your_resend_api_key"
   ```

4. **Deploy the Edge Function:**
   Deploy the function using the Supabase CLI:
   ```bash
   supabase deploy lemon-squeezy-webhook
   ```

---

## 🔗 Step 3: Wire up Checkout & Hosting Redirects

1. **Get Checkout Link:**
   In Lemon Squeezy, go to your product and copy the **Share / Checkout Link** (looks like `https://alhaqstudio.lemonsqueezy.com/checkout/buy/745c25cb-e4ad-4d97-a0b4-6abdafa7887d`).

2. **Update Hosting Redirects:**
   Open the `Studio-site` workspace (`d:\PROJECTS\Web\Studio-site\firebase.json`) and replace the destination with the actual checkout path from Lemon Squeezy:
   ```json
   "redirects": [
     {
       "source": "/amnshield-premium",
       "destination": "https://alhaqstudio.lemonsqueezy.com/checkout/buy/745c25cb-e4ad-4d97-a0b4-6abdafa7887d",
       "type": 302
     }
   ]
   ```
   Deploy the updated redirects to your hosting:
   ```bash
   firebase deploy --only hosting:main
   ```

3. **Configure Resend domain:**
   In your [Resend Dashboard](https://resend.com), register and verify the `alhaq.uk` domain so you can send emails from `noreply@alhaq.uk` (or update the `from` field in `index.ts` to match your verified domain).
