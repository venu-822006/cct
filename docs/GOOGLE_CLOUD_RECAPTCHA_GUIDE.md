# Setting Up reCAPTCHA & Bot Defense in Google Cloud

This guide explains how to add **Google reCAPTCHA & Bot Defense directly in Google Cloud** at the infrastructure layer (Google Cloud Armor + Cloud Load Balancing) to protect your Cloud Drive from automated tools, AI crawlers, and scrapers without requiring custom code inside the container.

---

## Why Add reCAPTCHA in Google Cloud?

By placing reCAPTCHA at the Google Cloud edge:
1. **Zero application code needed**: The application remains clean, fast, and instant for users.
2. **Blocked before reaching your server**: Suspicious bots, AI crawlers, and automated traffic are intercepted and served the Google reCAPTCHA image challenge (cars, crosswalks, traffic lights) at Google's global edge network before they ever hit Cloud Run.
3. **No extra compute cost**: Malicious requests do not consume Cloud Run CPU, memory, or billing quotas.

---

## Architecture

```
                    ┌────────────────────────────────────────┐
                    │      Internet / User / AI Bot          │
                    └───────────────────┬────────────────────┘
                                        │
                                        ▼
             ┌─────────────────────────────────────────────────────┐
             │       Google Cloud External HTTP(S) Load Balancer   │
             │                         +                           │
             │       Google Cloud Armor Security Policy            │
             │       (Rule: redirect_to_recaptcha)                 │
             └──────────────────────────┬──────────────────────────┘
                                        │
                         ┌──────────────┴──────────────┐
                         │                             │
                   [Human Passes]                 [Bot/Crawler]
                         │                             │
                         ▼                             ▼
             ┌─────────────────────────┐   ┌───────────────────────────┐
             │    Google Cloud Run     │   │ Intercepted with Google   │
             │      (Cloud Drive)      │   │ reCAPTCHA Image Challenge │
             │  - Instant Clean Access │   │ (Cars, Traffic lights)    │
             └─────────────────────────┘   └───────────────────────────┘
```

---

## Step 1: Create reCAPTCHA Enterprise Key in Google Cloud

1. Open the **Google Cloud Console** and select your project.
2. Enable the required APIs:
   ```bash
   gcloud services enable recaptchaenterprise.googleapis.com
   gcloud services enable compute.googleapis.com
   ```
3. In the Cloud Console, search for **reCAPTCHA Enterprise**.
4. Click **Create Key**:
   - **Key Name**: `cloud-drive-edge-key`
   - **Platform type**: **Web site**
   - **Domain List**: Add your domain (e.g. `*.run.app` or your custom domain).
   - **Integration type**: Select **Challenge** (Checkbox / Image selection challenge).
   - Click **Create Key** and copy the generated **Key ID**.

---

## Step 2: Create a Cloud Armor Security Policy

Create a security policy with a reCAPTCHA challenge rule that targets suspicious traffic, AI bots, and scrapers:

```bash
# 1. Create the Cloud Armor security policy
gcloud compute security-policies create cloud-drive-armor-policy \
  --description "Google Cloud Armor reCAPTCHA bot defense for Cloud Drive"

# 2. Add an edge reCAPTCHA challenge rule for bots and scrapers
gcloud compute security-policies rules create 1000 \
  --security-policy cloud-drive-armor-policy \
  --expression "request.headers['user-agent'].contains('bot') || request.headers['user-agent'].contains('crawler') || request.headers['user-agent'].contains('python') || request.headers['user-agent'].contains('curl')" \
  --action redirect-to-recaptcha \
  --recaptcha-action-token-site-keys "YOUR_RECAPTCHA_KEY_ID"

# 3. Add rate limiting rule (optional, challenges users exceeding 100 requests/minute)
gcloud compute security-policies rules create 2000 \
  --security-policy cloud-drive-armor-policy \
  --expression "true" \
  --action throttle \
  --rate-limit-threshold-count 100 \
  --rate-limit-threshold-interval-sec 60 \
  --conform-action allow \
  --exceed-action redirect-to-recaptcha \
  --enforce-on-key IP
```

---

## Step 3: Attach Cloud Armor to Cloud Run Backend Service

1. Create a serverless Network Endpoint Group (NEG) pointing to your Cloud Run service:
   ```bash
   gcloud compute network-endpoint-groups create cloud-drive-neg \
     --region=us-central1 \
     --network-endpoint-type=serverless \
     --cloud-run-service=cloud-drive
   ```

2. Create a backend service and attach the serverless NEG:
   ```bash
   gcloud compute backend-services create cloud-drive-backend \
     --global \
     --load-balancing-scheme=EXTERNAL_MANAGED

   gcloud compute backend-services add-backend cloud-drive-backend \
     --global \
     --network-endpoint-group=cloud-drive-neg \
     --network-endpoint-group-region=us-central1
   ```

3. Attach the Cloud Armor reCAPTCHA policy to the backend service:
   ```bash
   gcloud compute backend-services update cloud-drive-backend \
     --security-policy cloud-drive-armor-policy \
     --global
   ```

4. Create the URL map, HTTPS target proxy, and forwarding rule (Load Balancer IP):
   ```bash
   # URL Map
   gcloud compute url-maps create cloud-drive-url-map \
     --default-service cloud-drive-backend

   # Target HTTP(S) Proxy
   gcloud compute target-http-proxies create cloud-drive-http-proxy \
     --url-map cloud-drive-url-map

   # Forwarding Rule (Public IP)
   gcloud compute forwarding-rules create cloud-drive-forwarding-rule \
     --global \
     --target-http-proxy cloud-drive-http-proxy \
     --ports 80
   ```

---

## Outcome

- Real users visiting your site enjoy a clean, fast experience.
- Automated bots, crawlers, or inspection scripts hitting the Load Balancer are immediately challenged by Google Cloud with the authentic reCAPTCHA image selection challenge.
- Your Cloud Run application container does not need any local CAPTCHA code, making local development and testing fast and friction-free.
