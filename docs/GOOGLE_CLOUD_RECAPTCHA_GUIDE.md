# Deploying Google reCAPTCHA & Bot Defense in Google Cloud

This guide provides step-by-step instructions to configure **Google reCAPTCHA v2 ("I'm not a robot" with the visual image selection challenge)** and **Google Cloud Armor Bot Defense** on Google Cloud Run and Google Cloud Platform.

---

## Architecture Overview

```
                      ┌────────────────────────────────────────┐
                      │             User / Client              │
                      └──────────────────┬─────────────────────┘
                                         │
                        1. Renders reCAPTCHA v2 Checkbox
                        2. Image Challenge (Cars, Traffic Lights)
                                         │
                                         ▼
                      ┌────────────────────────────────────────┐
                      │    Google Cloud Run (Cloud Drive)      │
                      │   - Spring Boot + React                │
                      └──────────────────┬─────────────────────┘
                                         │
                        3. Server-side token verification
                                         │
                                         ▼
                      ┌────────────────────────────────────────┐
                      │  Google reCAPTCHA Verification API     │
                      │  https://www.google.com/recaptcha/api  │
                      └────────────────────────────────────────┘
```

---

## Step 1: Obtain Google reCAPTCHA v2 Keys

1. Open the [Google reCAPTCHA Admin Console](https://www.google.com/recaptcha/admin).
2. Click **+ (Create / Register a new site)**.
3. Configure the following fields:
   - **Label**: `Cloud Drive Production`
   - **reCAPTCHA type**: Select **reCAPTCHA v2** ➔ **"I'm not a robot" Checkbox** *(This produces the visual image selection challenge: cars, crosswalks, traffic lights, etc.)*.
   - **Domains**: Add:
     - `localhost`
     - `127.0.0.1`
     - Your Cloud Run domain (e.g. `*.run.app` or your custom domain `drive.yourdomain.com`).
   - Accept the Terms of Service and click **Submit**.
4. You will receive two keys:
   - **Site Key (Public)**: Used in the frontend web application.
   - **Secret Key (Private)**: Kept securely on the backend server.

> **Development Note**: By default, the app comes configured with Google's official public test site key (`6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI`), which displays the authentic Google checkbox and works on `localhost` and any domain.

---

## Step 2: Configure Environment Variables

### In Local Development / Docker
Set the following environment variables:
```bash
# Frontend Site Key (set in .env or build argument)
VITE_RECAPTCHA_SITE_KEY=YOUR_GOOGLE_SITE_KEY

# Backend Secret Key
RECAPTCHA_SECRET_KEY=YOUR_GOOGLE_SECRET_KEY
```

### In Google Cloud Run
Deploy or update your Cloud Run service with the secret key:
```bash
# Set project and region
gcloud config set project YOUR_PROJECT_ID
REGION="us-central1"

# Option A: Direct environment variables
gcloud run deploy cloud-drive \
  --image ${REGION}-docker.pkg.dev/YOUR_PROJECT_ID/cloud-drive-repo/cloud-drive:latest \
  --region ${REGION} \
  --set-env-vars "SPRING_PROFILES_ACTIVE=gcloud,RECAPTCHA_SECRET_KEY=YOUR_GOOGLE_SECRET_KEY"

# Option B: Recommended - Store in Google Secret Manager
echo -n "YOUR_GOOGLE_SECRET_KEY" | gcloud secrets create recaptcha-secret-key --data-file=-
gcloud secrets add-iam-policy-binding recaptcha-secret-key \
  --member="serviceAccount:YOUR_PROJECT_NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud run deploy cloud-drive \
  --region ${REGION} \
  --update-secrets "RECAPTCHA_SECRET_KEY=recaptcha-secret-key:latest"
```

---

## Step 3: Google Cloud Armor Bot Defense (Edge-Level Interception)

If you want Google Cloud to automatically intercept AI tools, scrapers, and malicious bots **at the network edge before traffic reaches your container**, use Google Cloud Armor with reCAPTCHA challenge.

### 1. Enable Cloud Armor and reCAPTCHA Enterprise
```bash
gcloud services enable compute.googleapis.com
gcloud services enable recaptchaenterprise.googleapis.com
```

### 2. Create a Cloud Armor Security Policy
```bash
gcloud compute security-policies create cloud-drive-bot-policy \
  --description "Protect Cloud Drive against AI scrapers and bot inspection"
```

### 3. Add reCAPTCHA Challenge Rule
Add a rule that triggers a reCAPTCHA challenge for suspicious traffic or high-rate automated tools:
```bash
# Challenge any request with high rate or matching known crawler user-agents
gcloud compute security-policies rules create 1000 \
  --security-policy cloud-drive-bot-policy \
  --expression "request.headers['user-agent'].contains('bot') || request.headers['user-agent'].contains('crawler') || request.headers['user-agent'].contains('python')" \
  --action redirect-to-recaptcha \
  --recaptcha-action-token-site-keys "YOUR_GOOGLE_SITE_KEY"
```

### 4. Attach Security Policy to Backend Service / Load Balancer
```bash
gcloud compute backend-services update cloud-drive-backend \
  --security-policy cloud-drive-bot-policy \
  --global
```

When an automated bot, AI crawler, or unverified inspector attempts to access the site, Google Cloud Armor will immediately block inspection and serve the Google reCAPTCHA image selection challenge shown below:

- Checkbox: **I'm not a robot**
- Challenge: **Select all images with cars / traffic lights**
- Once verified, the user is issued an exemption token and granted access to Cloud Drive.

---

## Step 4: Verification Checklist

1. **Frontend Display**: Open `http://localhost:5173/` or your Cloud Run URL. The "Sign In" and "Create Account" forms display the Google reCAPTCHA checkbox.
2. **Challenge Trigger**: Clicking the checkbox activates Google's verification engine (and image grid challenge if prompted).
3. **Backend Validation**: Submitting the form verifies the token against Google's verification API.
4. **Public Links Removed**: Public share links and the "Public links" sidebar have been removed from the application, keeping file sharing restricted to authenticated users.
