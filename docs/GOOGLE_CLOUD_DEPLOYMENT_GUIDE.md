# Google Cloud Platform (GCP) Complete Deployment Guide

This comprehensive guide covers how to deploy the **Personal Cloud Drive** application across Google Cloud Platform services using **Google Cloud Run (Serverless Container)**, **Google Artifact Registry**, **Google Cloud Build**, **Google Cloud Storage (GCS)**, and **Google Cloud Shell**.

---

## Architecture Overview

```
                                      ┌────────────────────────────────────────────────────────┐
                                      │              Google Cloud Global Edge                  │
                                      │  (HTTPS SSL + Optional Cloud Armor Bot Defense)        │
                                      └───────────────────────────┬────────────────────────────┘
                                                                  │
                                                                  ▼
                                      ┌────────────────────────────────────────────────────────┐
                                      │           Google Cloud Run Service                     │
                                      │              (cloud-drive:8080)                        │
                                      │  - React Frontend (Vite Single Page Application)       │
                                      │  - Spring Boot 3.3 Backend API (Java 21)               │
                                      │  - Auto-scaling (0 to 5 instances)                     │
                                      └──────────────┬─────────────────────────┬───────────────┘
                                                     │                         │
                                    Volume Mount (GCS FUSE)              JDBC Connection
                                                     │                         │
                                                     ▼                         ▼
                                      ┌────────────────────────┐    ┌──────────────────────────┐
                                      │  Google Cloud Storage  │    │  Persistent H2 / Cloud   │
                                      │     Bucket (GCS)       │    │      SQL (MySQL)         │
                                      │  (Users' stored files) │    │  (Users, Metadata, DB)   │
                                      └────────────────────────┘    └──────────────────────────┘
```

---

## Deployment Options

| Method | Target Service | Billing Required? | Persistence | Best For |
| :--- | :--- | :--- | :--- | :--- |
| **Method 1: Turnkey Cloud Run** | Cloud Run + Artifact Registry + Cloud Build | Yes (Free Tier eligible) | GCS Volume Mount or local | Full production scaling, public HTTPS domain |
| **Method 2: Google Cloud Shell** | Cloud Shell VM Web Preview | **NO (100% Free)** | 5 GB Persistent Home Dir | Immediate demo & evaluation without credit card |
| **Method 3: Cloud Build CI/CD** | Cloud Build + Cloud Run | Yes | Automated Git triggers | Continuous deployment on git push |

---

## Method 1: Turnkey One-Command Cloud Run Deployment (`deploy-gcloud.sh`)

The repository includes a production deployment script that automates the entire process:
1. Enables necessary Google Cloud APIs.
2. Creates an Artifact Registry Docker repository (`cloud-drive`).
3. Compiles the multi-stage container using Google Cloud Build.
4. Mounts a Google Cloud Storage bucket (optional, for persistent storage).
5. Deploys the service to Google Cloud Run and outputs the live HTTPS URL.

### Step 1: Open Google Cloud Shell or Terminal
Open [shell.cloud.google.com](https://shell.cloud.google.com) or your local terminal with `gcloud` installed.

### Step 2: Set Your GCP Project
```bash
# Verify your active project or set it:
gcloud config set project YOUR_PROJECT_ID
```

### Step 3: Run the Deployment Script
```bash
# Basic deployment:
bash deploy-gcloud.sh

# With persistent Google Cloud Storage bucket:
bash deploy-gcloud.sh --bucket YOUR_PROJECT_ID-cloud-drive-storage --region us-central1
```

### Script Execution Steps:
```
[1/6] Target Project: YOUR_PROJECT_ID (Region: us-central1)
[2/6] Enabling required Google Cloud APIs...
  ✓ Cloud Run, Artifact Registry, Cloud Build & Storage APIs enabled.
[3/6] Configuring Artifact Registry repository (cloud-drive)...
  ✓ Repository 'cloud-drive' already exists in us-central1.
[4/6] Building container image in Google Cloud Build...
  ✓ Image built and pushed to Artifact Registry.
[5/6] Configuring persistent Google Cloud Storage bucket...
  ✓ Configured GCS volume mount to /app/storage_root
[6/6] Deploying service 'cloud-drive' to Google Cloud Run...

========================================================
  ✓ DEPLOYMENT COMPLETE!
========================================================
Your Cloud Drive is live on Google Cloud at:
👉  https://cloud-drive-xxxx-uc.a.run.app

Default Administrator Credentials:
  Username: admin
  Password: admin123
```

---

## Method 2: Google Cloud Shell Instant Preview (100% Free, Zero Billing)

If your Google Cloud account does not have a credit card or billing account attached:

1. Open **Google Cloud Shell** at [shell.cloud.google.com](https://shell.cloud.google.com).
2. Clone your repository:
   ```bash
   git clone https://github.com/venu-822006/cct.git
   cd cct
   ```
3. Run the Cloud Shell launcher:
   ```bash
   bash deploy-cloudshell.sh
   ```
4. Click the **Web Preview** icon in the Cloud Shell top-right toolbar &rarr; **Preview on port 8080**.
5. Your Cloud Drive is live on `https://8080-cs-*.cloudshell.dev`!

---

## Method 3: Cloud Build CI/CD with `cloudbuild.yaml`

To build and deploy via Google Cloud Build triggers:

1. Submit the build manually at any time:
   ```bash
   gcloud builds submit --config=cloudbuild.yaml
   ```
2. Or connect your GitHub repository in the **Google Cloud Console**:
   - Go to **Cloud Build** &rarr; **Triggers**.
   - Click **Create Trigger**.
   - Event: **Push to a branch** (`^main$`).
   - Configuration: **Cloud Build configuration file (`cloudbuild.yaml`)**.
   - Now, every `git push origin main` automatically builds and deploys a fresh container to Cloud Run!

---

## Method 4: Configuring Google Cloud SQL (MySQL)

By default, Cloud Drive runs with a persistent, zero-setup embedded H2 database (`application-gcloud.yml`). If you want an enterprise-managed Google Cloud SQL instance:

1. **Create Cloud SQL MySQL Instance**:
   ```bash
   gcloud sql instances create cloud-drive-db \
     --database-version=MYSQL_8_0 \
     --tier=db-f1-micro \
     --region=us-central1
   ```
2. **Create Database & User**:
   ```bash
   gcloud sql databases create clouddrivedb --instance=cloud-drive-db
   gcloud sql users create driveuser --instance=cloud-drive-db --password=STRONG_PASSWORD
   ```
3. **Deploy Cloud Run with Cloud SQL Connection**:
   ```bash
   gcloud run deploy cloud-drive \
     --image us-central1-docker.pkg.dev/YOUR_PROJECT_ID/cloud-drive/app:latest \
     --add-cloudsql-instances YOUR_PROJECT_ID:us-central1:cloud-drive-db \
     --set-env-vars "SPRING_PROFILES_ACTIVE=gcloud,SPRING_DATASOURCE_URL=jdbc:mysql:///clouddrivedb?cloudSqlInstance=YOUR_PROJECT_ID:us-central1:cloud-drive-db&socketFactory=com.google.cloud.sql.mysql.SocketFactory,SPRING_DATASOURCE_USERNAME=driveuser,SPRING_DATASOURCE_PASSWORD=STRONG_PASSWORD" \
     --region us-central1
   ```

---

## Method 5: GitHub Container Registry (`ghcr.io`) Setup

If building images through GitHub Actions (`.github/workflows/docker.yaml`):

1. Go to your repository on GitHub: **Settings** &rarr; **Actions** &rarr; **General**.
2. Scroll down to **Workflow permissions**.
3. Select **Read and write permissions** and check **Allow GitHub Actions to create and approve pull requests**.
4. Click **Save**.
5. Now, pushing to `main` will successfully publish the container image to `ghcr.io/venu-822006/cct:latest`.

---

## Method 6: Edge reCAPTCHA & Bot Defense in Google Cloud

To protect your Cloud Run deployment from automated bots, AI crawlers, and scrapers without adding any code to your container:
- Refer to the dedicated guide: [`docs/GOOGLE_CLOUD_RECAPTCHA_GUIDE.md`](./GOOGLE_CLOUD_RECAPTCHA_GUIDE.md).
- Uses **Google Cloud Armor** + **Cloud Load Balancing** with `redirect_to_recaptcha` to serve the Google image challenge (cars, crosswalks) directly at Google's global edge network.

---

## Verification & Health Check

Once deployed to Cloud Run, you can verify your service status:
```bash
# Check service health:
gcloud run services describe cloud-drive --region=us-central1

# View live container logs:
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=cloud-drive" --limit 20
```
