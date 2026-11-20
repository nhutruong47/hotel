#!/bin/sh
# =============================================================================
# Zero-surprise deploy on the VPS (run from the repository checkout):
#   ops/deploy/deploy.sh <image-tag>
#
#   1. on-demand database backup (restore point for this release)
#   2. pull the backend/frontend images for <image-tag> from the registry
#   3. docker compose up -d (Flyway migrates on backend start)
#   4. wait until the backend container reports healthy
#   5. on failure: roll the containers back to the previous images and exit 1
#      (database changes are NOT rolled back automatically - see
#      docs/operations/runbook.md "Failed deploy")
#
# Env: ENV_FILE (.env.prod), REGISTRY (ghcr.io/<owner>), HEALTH_TIMEOUT (300 s)
# =============================================================================
set -eu

TAG="${1:?usage: deploy.sh <image-tag>}"
cd "$(dirname "$0")/../.."
ENV_FILE="${ENV_FILE:-.env.prod}"
REGISTRY="${REGISTRY:?REGISTRY is required, e.g. ghcr.io/owner}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-300}"

[ -f "$ENV_FILE" ] || { echo "[deploy] $ENV_FILE not found" >&2; exit 2; }

compose() {
    docker compose --env-file "$ENV_FILE" -f docker-compose.yml -f docker-compose.prod.yml "$@"
}

current_image() {
    id="$(compose ps -q "$1" 2>/dev/null || true)"
    if [ -n "$id" ]; then
        docker inspect --format '{{.Config.Image}}' "$id" 2>/dev/null || true
    fi
}

wait_healthy() {
    elapsed=0
    while [ "$elapsed" -lt "$HEALTH_TIMEOUT" ]; do
        id="$(compose ps -q backend)"
        status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$id" 2>/dev/null || echo missing)"
        [ "$status" = "healthy" ] && return 0
        [ "$status" = "unhealthy" ] && return 1
        sleep 5
        elapsed=$((elapsed + 5))
    done
    return 1
}

PREV_BACKEND="$(current_image backend)"
PREV_FRONTEND="$(current_image frontend)"
echo "[deploy] previous images: backend=${PREV_BACKEND:-none} frontend=${PREV_FRONTEND:-none}"

if [ -n "$(compose ps -q backup 2>/dev/null || true)" ]; then
    echo "[deploy] pre-deploy backup"
    compose exec -T backup sh -c '. /etc/backup.env && /bin/sh /ops/backup/pg-backup.sh'
else
    echo "[deploy] backup service not running yet (first deploy) - skipping pre-deploy backup"
fi

BACKEND_IMAGE="$REGISTRY/hotel-backend:$TAG"
FRONTEND_IMAGE="$REGISTRY/hotel-frontend:$TAG"
export BACKEND_IMAGE FRONTEND_IMAGE
echo "[deploy] deploying $BACKEND_IMAGE and $FRONTEND_IMAGE"
compose pull backend frontend
compose up -d --no-build --remove-orphans

if wait_healthy; then
    echo "[deploy] backend healthy - release $TAG is live"
    exit 0
fi

echo "[deploy] backend did not become healthy within ${HEALTH_TIMEOUT}s - rolling back" >&2
compose logs --tail=200 backend >&2 || true
if [ -n "$PREV_BACKEND" ] && [ -n "$PREV_FRONTEND" ]; then
    BACKEND_IMAGE="$PREV_BACKEND"
    FRONTEND_IMAGE="$PREV_FRONTEND"
    export BACKEND_IMAGE FRONTEND_IMAGE
    compose up -d --no-build backend frontend
    echo "[deploy] rolled back to $PREV_BACKEND / $PREV_FRONTEND" >&2
fi
exit 1
