#!/usr/bin/env bash
# Stops Cloud Drive background service on port 8080
echo "Stopping Cloud Drive on port 8080..."
fuser -k 8080/tcp 2>/dev/null || true
pkill -f personal-cloud-drive 2>/dev/null || true
rm -f app.pid 2>/dev/null || true
echo "✓ Cloud Drive stopped."
