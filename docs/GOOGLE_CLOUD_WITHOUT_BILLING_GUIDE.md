# Guide: Implementing & Running on Google Cloud Without a Billing Account

## 1. The Reality of Google Cloud Platform (GCP) Billing

When using Google Cloud Platform (GCP), services such as **Cloud Run**, **Compute Engine (VMs)**, **Google Kubernetes Engine (GKE)**, and **App Engine** strictly require linking a credit card or active billing account—even to access their free tiers. If you do not attach a billing account, Google Console blocks deployment with:

> *"To use this product, you must enable Billing on your project."*

However, there are **two official Google-provided methods** and **two zero-cost cloud tunneling/hosting methods** to implement and run this project without ever entering credit card details or creating a billing account:

---

## Method 1: Google Cloud Shell (Official Google Cloud — 100% Free & Zero Billing)

Google Cloud Shell is an official, interactive cloud environment provided by Google for every Google account.

### Why Cloud Shell Works Without Billing:
- **Zero Billing Required:** Never asks for a credit card or bank details.
- **Pre-installed Tools:** Java 21, Maven, Node.js, Git, and Docker are already installed.
- **5 GB Persistent Home Directory:** Files and databases persist across sessions.
- **Web Preview Proxy:** Google provides a built-in proxy that exposes port `8080` via a secure Google-hosted domain (`https://*-*-*.cloudshell.dev`).

### Step-by-Step Instructions:

1. **Open Google Cloud Shell:**
   - Go to [shell.cloud.google.com](https://shell.cloud.google.com) and log in with your Google account.

2. **Upload or Clone the Project:**
   ```bash
   # Option A: Clone from your GitHub repository
   git clone <your-repository-url> personal-cloud-drive
   cd personal-cloud-drive

   # Option B: Or click "More" (three dots) in Cloud Shell -> "Upload" -> select your project zip
   ```

3. **Run the One-Click Cloud Shell Launcher:**
   ```bash
   chmod +x deploy-cloudshell.sh
   ./deploy-cloudshell.sh
   ```

4. **Open the Web Preview in Your Browser:**
   - In the top-right corner of Google Cloud Shell, click the **Web Preview** icon (looks like an eye with a window).
   - Click **"Change Port"** and type `8080`.
   - Click **"Change and Preview"**.
   - Your **Personal Cloud Drive** will open in a new browser tab running live on Google Cloud infrastructure!

---

## Method 2: Google Drive API as Storage Backend (GCP Console Without Billing)

You can use Google Cloud Console to enable Google APIs for personal cloud storage without a billing account:

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click **Select a Project** &rarr; **New Project** (e.g., `Personal-Cloud-Drive`).  
   *(Notice: Google allows creating up to 12 projects without a billing account!)*
3. Navigate to **APIs & Services** &rarr; **Library**.
4. Search for **Google Drive API** and click **Enable**.
5. Go to **OAuth Consent Screen**:
   - Choose **External**.
   - App Name: `Personal Cloud Drive`.
   - Under **Publishing Status**, leave it as **Testing** (no billing or verification required).
   - Add your own email under **Test Users**.
6. Go to **Credentials** &rarr; **Create Credentials** &rarr; **OAuth Client ID**:
   - Application Type: **Web Application**.
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`.
7. Download the `client_secret.json`.  
   *Your app can now authenticate and sync files with 15 GB of Google Drive cloud storage without paying a single cent.*

---

## Method 3: Exposing to a Free Global Public URL (Cloudflare Tunnel)

If you want your application accessible to anyone on the internet (e.g. university evaluators, peers, or external testing) from your local computer or Cloud Shell without paying GCP:

1. **Download Cloudflare Tunnel (`cloudflared`):**
   - On Windows: Download `cloudflared.exe` from [Cloudflare Releases](https://github.com/cloudflare/cloudflared/releases) or run:
     ```powershell
     winget install Cloudflare.cloudflared
     ```
   - On Linux / Google Cloud Shell:
     ```bash
     curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
     sudo dpkg -i cloudflared.deb
     ```

2. **Expose Your Running Drive:**
   Make sure Personal Cloud Drive is running on port 8080, then execute:
   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

3. **Get Your Free Live HTTPS URL:**
   Cloudflare outputs a public URL:
   ```
   https://random-words-generated.trycloudflare.com
   ```
   Anyone in the world can open this link on their phone or laptop and use your Personal Cloud Drive with full SSL encryption! **No billing account, credit card, or domain purchase required.**

---

## Method 4: 100% Free Cloud Container Hosting (No Credit Card)

If you want a permanent 24/7 cloud deployment without a billing account, use these container platforms with the provided `Dockerfile`:

### Option A: Hugging Face Spaces (Docker Space)
- **Specs:** 16 GB RAM, 2 vCPUs, 50 GB disk, 100% free forever.
- **Card Required?** No credit card required.
- **Steps:**
  1. Go to [huggingface.co/spaces](https://huggingface.co/spaces) and click **Create new Space**.
  2. Select **Docker** as the SDK.
  3. Push your repository.
  4. The multi-stage `Dockerfile` will automatically build the React frontend and Spring Boot backend and host it on a public HTTPS URL.

### Option B: Render.com
- **Specs:** Free Web Service tier (512 MB RAM, free SSL, automatic git deploys).
- **Card Required?** No credit card required for standard free web services.
- **Steps:**
  1. Sign in to [render.com](https://render.com) using your GitHub account.
  2. Click **New +** &rarr; **Web Service** &rarr; Connect your repository.
  3. Environment: **Docker**.
  4. Port: `8080`.
  5. Click **Create Web Service**.

---

## Summary Comparison

| Platform / Method | Needs Billing / Credit Card? | Storage / Specs | URL Type | Best For |
| :--- | :--- | :--- | :--- | :--- |
| **Google Cloud Shell** | **NO (100% Free)** | 5 GB Persistent, 16 GB RAM | `*.cloudshell.dev` | Direct Google Cloud evaluation & testing |
| **Google Drive API** | **NO (100% Free)** | 15 GB Google Drive Storage | OAuth2 Flow | Cloud storage backend |
| **Cloudflare Tunnel** | **NO (100% Free)** | Depends on Host Machine | `*.trycloudflare.com` | Instant live external demo to anyone |
| **Hugging Face Spaces**| **NO (100% Free)** | 16 GB RAM, 2 vCPU, 50 GB Disk | `*.hf.space` | Permanent 24/7 free cloud deployment |
| **Standard GCP (Cloud Run)** | **YES (Credit Card Required)** | Free Tier Quota | `*.run.app` | Enterprise production scaling |
