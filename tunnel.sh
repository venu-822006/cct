#!/usr/bin/env bash
# ===================================================================
# Cloud Drive — Instant Public HTTPS Tunnel (Zero Proxy / Zero CAPTCHA)
# Exposes port 8080 directly to a public *.trycloudflare.com HTTPS URL
# Usage: bash tunnel.sh
# ===================================================================

set -e

CLOUDFLARED_BIN="$HOME/bin/cloudflared"

if ! command -v cloudflared &>/dev/null; then
  if [ ! -f "$CLOUDFLARED_BIN" ]; then
    echo "Downloading cloudflared tunnel client..."
    mkdir -p "$HOME/bin"
    curl -sSL "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64" -o "$CLOUDFLARED_BIN"
    chmod +x "$CLOUDFLARED_BIN"
  fi
  export PATH="$HOME/bin:$PATH"
fi

echo "=========================================================="
echo " Starting Public HTTPS Tunnel for Cloud Drive (Port 8080)"
echo "=========================================================="
echo " Benefits:"
echo "   ✓ Real public HTTPS URL (https://*.trycloudflare.com)"
echo "   ✓ Bypasses Cloud Shell Web Preview proxies & redirects"
echo "   ✓ Zero Google CAPTCHA blocks"
echo "   ✓ No billing account required"
echo "=========================================================="
echo ""

cloudflared tunnel --url http://localhost:8080
