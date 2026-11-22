#!/usr/bin/env bash
# ===================================================================
# Personal Cloud Drive - Google Cloud Shell Launcher (Zero Billing)
# ===================================================================

set -e

echo "=== 1/4 Checking Java and Node environment in Google Cloud Shell ==="
java -version
node -v

echo "=== 2/4 Building React Frontend Assets ==="
if [ -f "package-lock.json" ]; then
    npm ci
else
    npm install
fi
npm run build

echo "=== 3/4 Copying Frontend Dist into Spring Boot Static Resources ==="
mkdir -p backend/src/main/resources/static
cp -r dist/* backend/src/main/resources/static/

echo "=== 4/4 Packaging and Starting Spring Boot Cloud Drive on Port 8080 ==="
cd backend
chmod +x ./mvnw || true
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
else
    mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
fi
