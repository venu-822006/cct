# Cloud-Native Free Tier Architecture: Google Cloud & Cloudflare

This document details the production deployment architecture for **CCT – Personal Cloud Drive** utilizing **only free cloud services** from **Google Cloud** and **Cloudflare**, eliminating reliance on manual terminal sessions, ephemeral scripts, or ad-hoc local processes.

---

## 1. Architecture Overview

```
                                  ┌────────────────────────────────────────┐
                                  │            Client Browser              │
                                  └───────────────────┬────────────────────┘
                                                      │
                                                      │ HTTPS (Anycast)
                                                      ▼
                       ┌──────────────────────────────────────────────────────────────┐
                       │                   Cloudflare Edge Network                    │
                       │                                                              │
                       │  ┌─────────────────────────┐   ┌──────────────────────────┐  │
                       │  │    Cloudflare Pages     │   │ Cloudflare Pages Function│  │
                       │  │   Static React Frontend │   │   Edge Reverse Proxy     │  │
                       │  │    (Global Edge CDN)    │   │  (functions/api/[[path]]) │  │
                       │  └─────────────────────────┘   └─────────────┬────────────┘  │
                       │                                              │               │
                       │  ┌─────────────────────────┐                 │               │
                       │  │   Cloudflare Turnstile  │                 │               │
                       │  │   Free Bot Protection   │                 │               │
                       │  └─────────────────────────┘                 │               │
                       └──────────────────────────────────────────────┼───────────────┘
                                                                      │
                                                Secure Edge Forwarding│ HTTPS
                                                                      ▼
                       ┌──────────────────────────────────────────────────────────────┐
                       │                     Google Cloud Platform                    │
                       │                                                              │
                       │  ┌────────────────────────────────────────────────────────┐  │
                       │  │               Google Cloud Run (Serverless)            │  │
                       │  │  - Always Free Tier: 2M req/mo, 360k GB-s RAM          │  │
                       │  │  - Scale-to-zero (0 instances when idle = $0 cost)     │  │
                       │  │  - Spring Boot Backend + Embedded Static Fallback      │  │
                       │  │  - Probes: /actuator/health (Liveness & Startup)       │  │
                       │  └───────────────────────────┬────────────────────────────┘  │
                       │                              │                               │
                       │  ┌───────────────────────────┴────────────────────────────┐  │
                       │  │               Google Cloud Storage (GCS)               │  │
                       │  │  - 5 GB-months Always Free Tier (US regions)           │  │
                       │  │  - Mounted via Cloud Run GCSFuse at /app/storage_root  │  │
                       │  └────────────────────────────────────────────────────────┘  │
                       │                                                              │
                       │  ┌────────────────────────────────────────────────────────┐  │
                       │  │             Google Cloud Build (CI/CD Pipeline)        │  │
                       │  │  - 120 free build-minutes per day                      │  │
                       │  │  - Automated multi-stage Docker build & deploy         │  │
                       │  └────────────────────────────────────────────────────────┘  │
                       └──────────────────────────────────────────────────────────────┘
```

---

## 2. Cloudflare Free Cloud Services

| Service | Free Tier Allowance | Project Implementation |
|---|---|---|
| **Cloudflare Pages** | Unlimited static requests, 500 builds/mo, 100 custom domains | `wrangler.toml`, `public/_headers`, `public/_routes.json` |
| **Pages Functions** | 100,000 edge invocations / day | `functions/api/[[path]].ts` edge reverse proxy |
| **Cloudflare Turnstile** | **100% Free** (Unlimited site validations, zero CAPTCHA puzzle friction) | `TurnstileService.java`, `@PostMapping("/verify-turnstile")`, dynamic modal in `AuthModal.tsx` |
| **Cloudflare R2** | 10 GB free object storage, 0 egress fees, 1M Class A & 10M Class B ops/mo | S3-compatible cloud storage adapter |
| **Cloudflare Zero Trust Tunnel** | Free for up to 50 users, unlimited secure tunnels | `cloudflared/docker-compose.tunnel.yml` automated daemon |

### Key Advantages of Cloudflare Services:
1. **No CORS / Auth Redirect Hassles**:
   - Because `functions/api/[[path]].ts` proxies `/api/**` at the same origin as the frontend (`*.pages.dev`), browser requests never trigger CORS preflight rejections or Google Cloud Shell 302 login redirects.
2. **Streaming Uploads & Downloads**:
   - The edge function uses standard `duplex: "half"` streaming, allowing multi-megabyte file uploads and chunked downloads without buffer bloat.
3. **Turnstile vs reCAPTCHA**:
   - reCAPTCHA Free is limited to 10k assessments/month and frequently presents difficult image challenges.
   - Cloudflare Turnstile provides unlimited assessments, zero image puzzles, and high user privacy. The application automatically detects and supports both.

---

## 3. Google Cloud Free Cloud Services

| Service | Free Tier Allowance | Project Implementation |
|---|---|---|
| **Google Cloud Run** | 2 million requests/mo, 360,000 GB-sec RAM, 180,000 vCPU-sec | `gcloud-run.yaml`, scales to 0 instances when idle |
| **Google Cloud Storage (GCS)**| 5 GB-months free tier in `us-central1`, `us-east1`, or `us-west1` | Mounted via Cloud Run GCSFuse at `/app/storage_root` |
| **Google Cloud Build** | 120 free build-minutes per day | `cloudbuild.yaml` automated container compilation & deployment |
| **Spring Boot Actuator** | Built-in production health metrics | `/actuator/health` endpoint for Cloud Run liveness/readiness probes |

### Zero-Cost Autoscaling:
- Cloud Run containers automatically scale down to **0 instances** when no requests are pending.
- Cold start delay is minimized via Spring Boot JRE Alpine container packaging (~15s initial start).
- Startup probes (`/actuator/health`) ensure Cloud Run does not route traffic until the Spring Boot application context is completely initialized.

---

## 4. Automated CI/CD Pipelines

### 4.1 Cloudflare Pages (`.github/workflows/deploy-cloudflare-pages.yaml`)
- **Trigger**: Push to `main` branch when frontend code changes.
- **Action**: Runs `bun install` & `bun run build`, then publishes output directory `dist` to Cloudflare Pages edge network via `cloudflare/wrangler-action`.

### 4.2 Google Cloud Run (`.github/workflows/deploy-gcloud-run.yaml`)
- **Trigger**: Push to `main` branch when backend code changes.
- **Action**: Authenticates with Google Cloud via Service Account or Workload Identity, builds multi-stage Docker image, pushes to Artifact Registry, and updates Cloud Run service.

### 4.3 Google Cloud Build (`cloudbuild.yaml`)
- Native Google Cloud Build pipeline triggering directly from GitHub repo connections in the Google Cloud Console with zero external dependencies.

---

## 5. Cloudflare Zero Trust Tunnel Daemon (`cloudflared`)

For self-hosted hybrid or edge-to-origin connectivity:
- Defined in `cloudflared/docker-compose.tunnel.yml`.
- Runs `cloudflare/cloudflared:latest` in background daemon mode with `restart: always`.
- Automatically establishes an outbound encrypted QUIC/HTTP2 tunnel to Cloudflare edge.
- No public IP address or open firewall ports required on the host.

Run command:
```bash
CLOUDFLARE_TUNNEL_TOKEN="<your-tunnel-token>" docker compose -f cloudflared/docker-compose.tunnel.yml up -d
```

---

## 6. Deployment Verification & Status Checklist

| Component | Status | Verification Check |
|---|---|---|
| Cloudflare Pages Edge Config | Verified | `wrangler.toml`, `public/_headers`, `public/_routes.json` |
| Edge API Proxy | Verified | `functions/api/[[path]].ts` |
| Cloudflare Turnstile Integration | Verified | `TurnstileService.java`, passing tests |
| Cloud Run Service Spec | Verified | `gcloud-run.yaml` (512Mi RAM, 1 vCPU, minScale: 0) |
| Cloud Build CI/CD Spec | Verified | `cloudbuild.yaml` |
| Spring Boot Health Actuator | Verified | `/actuator/health` passing in `mvnw test` |
| Cloudflare Tunnel Daemon | Verified | `cloudflared/docker-compose.tunnel.yml` |
| Unit & Integration Tests | Passed | 9/9 Bun unit tests + 12/12 Spring Boot tests passing |
