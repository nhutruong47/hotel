#!/bin/sh
# Bootstrap a temporary self-signed certificate so nginx can start before
# Let's Encrypt has issued the real one (first deploy only).
# Usage: ops/nginx/init-dummy-cert.sh <server_name> [letsencrypt_dir]
set -eu

SERVER_NAME="${1:?usage: init-dummy-cert.sh <server_name> [letsencrypt_dir]}"
LE_DIR="${2:-./ops/certbot/conf}"
LIVE="$LE_DIR/live/$SERVER_NAME"

if [ -f "$LIVE/fullchain.pem" ]; then
    echo "Certificate already present in $LIVE - nothing to do."
    exit 0
fi

mkdir -p "$LIVE"
openssl req -x509 -nodes -newkey rsa:2048 -days 2 \
    -keyout "$LIVE/privkey.pem" \
    -out "$LIVE/fullchain.pem" \
    -subj "/CN=$SERVER_NAME" >/dev/null 2>&1
echo "Temporary self-signed certificate written to $LIVE (valid 2 days)."
echo "Next: start nginx, then issue the real certificate with certbot (see docs/operations/deployment-guide.md)."
