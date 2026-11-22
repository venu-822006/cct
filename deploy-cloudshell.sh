#!/usr/bin/env bash
# ===================================================================
# Cloud Drive — Google Cloud Shell Launcher
# Run this script inside Google Cloud Shell to build and start the app
# Usage: bash deploy-cloudshell.sh [--project PROJECT_ID]
# ===================================================================

set -e

# ── Parse args ──────────────────────────────────────────────────────
PROJECT_ID=""
while [[ "$#" -gt 0 ]]; do
  case $1 in
    --project) PROJECT_ID="$2"; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
  shift
done

if [ -n "$PROJECT_ID" ]; then
  gcloud config set project "$PROJECT_ID"
fi

echo "=== Cloud Drive — Cloud Shell Build & Run ==="
echo ""

# ── Environment check ───────────────────────────────────────────────
echo "[1/5] Checking environment..."
java -version 2>&1 | head -1
node --version
echo ""

# ── Frontend build ───────────────────────────────────────────────────
echo "[2/5] Building React frontend..."
if [ -f "package-lock.json" ]; then
    npm ci --silent
else
    npm install --silent
fi
npm run build
echo "  ✓ Frontend built → dist/"
echo ""

# ── Copy frontend into Spring Boot static resources ──────────────────
echo "[3/5] Copying frontend into Spring Boot static resources..."
mkdir -p backend/src/main/resources/static
rm -rf backend/src/main/resources/static/*
cp -r dist/* backend/src/main/resources/static/
echo "  ✓ Copied to backend/src/main/resources/static/"
echo ""

# ── Build Spring Boot backend ────────────────────────────────────────
echo "[4/5] Building Spring Boot backend..."
cd backend
chmod +x ./mvnw 2>/dev/null || true
if [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests -q
else
    mvn clean package -DskipTests -q
fi
cd ..
echo "  ✓ JAR built → backend/target/"
echo ""

# ── Start the app ────────────────────────────────────────────────────
echo "[5/5] Starting Cloud Drive on port 8080..."
echo "  Checking and freeing port 8080..."
fuser -k 8080/tcp 2>/dev/null || true
pkill -f personal-cloud-drive 2>/dev/null || true
sleep 1

echo "  Open the Web Preview (port 8080) in Cloud Shell toolbar"
echo ""

JAR=$(find backend/target -maxdepth 1 -name "*.jar" ! -name "*.original" | head -1)
if [ -z "$JAR" ]; then
  JAR=$(ls backend/target/*.jar | head -1)
fi
echo "  Starting JAR: $JAR"
exec java \
  -Dserver.port=8080 \
  -Dspring.profiles.active=gcloud \
  -Djava.security.egd=file:/dev/./urandom \
  -jar "$JAR"
