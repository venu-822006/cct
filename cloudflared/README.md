# Cloudflare Zero Trust Free Tunnel Daemon

This directory provides an automated, background daemon for Cloudflare Zero Trust Tunnels (100% free service).

### Why use Cloudflare Tunnel instead of manual Cloud Shell / port forwarding?
- **Zero Ephemeral Disconnects**: Runs as a daemon with `restart: always`. It will never stop when a terminal window is closed.
- **No Open Ports**: No firewall rules, router port-forwarding, or public IP addresses required.
- **Enterprise-Grade Edge CDN & SSL**: Cloudflare terminates HTTPS automatically at its global edge with free SSL certificates.
- **100% Free**: Included on Cloudflare's free plan.

### Quick Start with Docker Daemon
1. Go to [Cloudflare Zero Trust Dashboard](https://one.dash.cloudflare.com/) -> **Networks** -> **Tunnels**.
2. Click **Add a tunnel**, choose **Cloudflare Tunnel**, name it (e.g. `cct-drive-tunnel`), and save.
3. Under "Install and run a connector", copy the provided token (looks like `eyJh...`).
4. Run the daemon on your machine:
   ```bash
   CLOUDFLARE_TUNNEL_TOKEN="your_token_here" docker compose -f cloudflared/docker-compose.tunnel.yml up -d
   ```
5. In the Cloudflare dashboard, add a Public Hostname:
   - Subdomain: `drive` (or `@` for root)
   - Domain: your domain
   - Service: `HTTP://host.docker.internal:8080` (or `HTTP://localhost:8080`)
6. Your Personal Cloud Drive is now securely online 24/7!
