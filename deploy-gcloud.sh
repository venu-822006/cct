#!/usr/bin/env bash
# ===================================================================
# Cloud Drive — Google Cloud Platform Production Deployer
# Deploys Cloud Drive to Google Cloud Run + Artifact Registry + Cloud Storage
#
# Usage:
#   bash deploy-gcloud.sh [--project PROJECT_ID] [--region REGION] [--bucket BUCKET_NAME]
#
# Prerequisites:
#   - Google Cloud SDK (gcloud CLI) installed and logged in (`gcloud auth login`)
#   - A GCP Project with billing enabled
# ===================================================================

set -e

# ── Defaults ────────────────────────────────────────────────────────
DEFAULT_REGION="us-central1"
DEFAULT_REPO="cloud-drive"
DEFAULT_SERVICE="cloud-drive"

PROJECT_ID=""
REGION="$DEFAULT_REGION"
BUCKET_NAME=""

# ── Parse Arguments ────────────────────────────────────────────────
while [[ "$#" -gt 0 ]]; do
  case $1 in
    --project) PROJECT_ID="$2"; shift ;;
    --region) REGION="$2"; shift ;;
    --bucket) BUCKET_NAME="$2"; shift ;;
    -h|--help)
      echo "Cloud Drive - Google Cloud Deployment Script"
      echo ""
      echo "Options:"
      echo "  --project <PROJECT_ID>    Google Cloud Project ID (defaults to active gcloud project)"
      echo "  --region <REGION>         Google Cloud region (default: us-central1)"
      echo "  --bucket <BUCKET_NAME>    Optional GCS bucket name for persistent file storage"
      echo "  -h, --help                Show this help message"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
  shift
done

echo "========================================================"
echo "    Cloud Drive — Google Cloud Production Deployment"
echo "========================================================"
echo ""

# ── 1. Detect & Validate Project ID ─────────────────────────────────
if [ -z "$PROJECT_ID" ]; then
  PROJECT_ID=$(gcloud config get-value project 2>/dev/null || true)
fi

if [ -z "$PROJECT_ID" ] || [ "$PROJECT_ID" = "(unset)" ]; then
  echo "Error: No Google Cloud project selected."
  echo "Run: gcloud config set project YOUR_PROJECT_ID"
  echo "Or pass: bash deploy-gcloud.sh --project YOUR_PROJECT_ID"
  exit 1
fi

echo "[1/6] Target Project: $PROJECT_ID (Region: $REGION)"
gcloud config set project "$PROJECT_ID" --quiet

# ── 2. Enable Required Google Cloud APIs ────────────────────────────
echo "[2/6] Enabling required Google Cloud APIs..."
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  storage.googleapis.com \
  --quiet
echo "  ✓ Cloud Run, Artifact Registry, Cloud Build & Storage APIs enabled."

# ── 3. Create Artifact Registry Repository ─────────────────────────
echo "[3/6] Configuring Artifact Registry repository ($DEFAULT_REPO)..."
if ! gcloud artifacts repositories describe "$DEFAULT_REPO" --location="$REGION" &>/dev/null; then
  echo "  Creating repository '$DEFAULT_REPO' in $REGION..."
  gcloud artifacts repositories create "$DEFAULT_REPO" \
    --repository-format=docker \
    --location="$REGION" \
    --description="Cloud Drive Container Images" \
    --quiet
else
  echo "  ✓ Repository '$DEFAULT_REPO' already exists in $REGION."
fi

IMAGE_TAG="${REGION}-docker.pkg.dev/${PROJECT_ID}/${DEFAULT_REPO}/app:latest"

# ── 4. Build and Push Container using Cloud Build ───────────────────
echo "[4/6] Building container image in Google Cloud Build..."
echo "  Target image: $IMAGE_TAG"
gcloud builds submit --tag "$IMAGE_TAG" .
echo "  ✓ Image built and pushed to Artifact Registry."

# ── 5. (Optional) Create Cloud Storage Bucket for File Persistence ─
EXTRA_DEPLOY_FLAGS=()
if [ -n "$BUCKET_NAME" ]; then
  echo "[5/6] Configuring persistent Google Cloud Storage bucket ($BUCKET_NAME)..."
  if ! gsutil ls -b "gs://${BUCKET_NAME}" &>/dev/null; then
    echo "  Creating GCS bucket 'gs://${BUCKET_NAME}' in $REGION..."
    gsutil mb -p "$PROJECT_ID" -c standard -l "$REGION" "gs://${BUCKET_NAME}"
  fi
  EXTRA_DEPLOY_FLAGS+=(
    "--add-volume" "name=drive-storage,type=cloud-storage,bucket=${BUCKET_NAME}"
    "--add-volume-mount" "volume=drive-storage,mount-path=/app/storage_root"
  )
  echo "  ✓ Configured GCS volume mount to /app/storage_root"
else
  echo "[5/6] Skipping GCS volume mount (using standard local container storage)."
  echo "  Tip: Pass --bucket <NAME> to mount a permanent Cloud Storage bucket."
fi

# ── 6. Deploy to Google Cloud Run ───────────────────────────────────
echo "[6/6] Deploying service '$DEFAULT_SERVICE' to Google Cloud Run..."
gcloud run deploy "$DEFAULT_SERVICE" \
  --image "$IMAGE_TAG" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 5 \
  --set-env-vars "SPRING_PROFILES_ACTIVE=gcloud" \
  "${EXTRA_DEPLOY_FLAGS[@]}" \
  --quiet

echo ""
echo "========================================================"
echo "  ✓ DEPLOYMENT COMPLETE!"
echo "========================================================"
SERVICE_URL=$(gcloud run services describe "$DEFAULT_SERVICE" --region="$REGION" --format="value(status.url)")
echo ""
echo "Your Cloud Drive is live on Google Cloud at:"
echo "👉  $SERVICE_URL"
echo ""
echo "Default Administrator Credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo "========================================================"
