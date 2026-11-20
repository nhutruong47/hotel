#!/bin/sh
# =============================================================================
# Nightly PostgreSQL backup (RPO <= 24h, ADR-0009 Q10).
# Runs in the "backup" service of docker-compose.prod.yml (postgres:16-alpine).
#
#   1. pg_dump custom format to a .partial file
#   2. verify the archive is readable (pg_restore --list) before publishing it
#   3. checksum, retention, optional off-site copy
#   4. publish hotel_backup_* metrics for Prometheus (node-exporter textfile
#      collector) -> alert BackupMissing fires if no success for 26h
#
# Env: PGHOST PGPORT PGUSER PGPASSWORD PGDATABASE (libpq),
#      BACKUP_DIR (/backups), BACKUP_RETENTION_DAYS (14), METRICS_DIR (optional),
#      BACKUP_OFFSITE_CMD (optional, receives the dump path as $1,
#      e.g. 'rclone copy "$1" remote:nhu-villas-backups').
# =============================================================================
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
METRICS_DIR="${METRICS_DIR:-}"
DB="${PGDATABASE:-hotel}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FINAL="$BACKUP_DIR/$DB-$STAMP.dump"
PARTIAL="$FINAL.partial"

log() { echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) [pg-backup] $*"; }

write_metric() {
    # $1 = file name, $2 = metric body. Separate files for success and failure so a
    # failed run never erases the last-success timestamp the BackupMissing alert uses.
    # Written atomically so node-exporter never reads half a file.
    [ -n "$METRICS_DIR" ] || return 0
    mkdir -p "$METRICS_DIR"
    printf '%s\n' "$2" > "$METRICS_DIR/$1.$$"
    mv "$METRICS_DIR/$1.$$" "$METRICS_DIR/$1"
}

on_failure() {
    rm -f "$PARTIAL"
    log "FAILED"
    write_metric hotel_backup_failure.prom "# HELP hotel_backup_last_failure_timestamp_seconds Last failed backup (unix time)
# TYPE hotel_backup_last_failure_timestamp_seconds gauge
hotel_backup_last_failure_timestamp_seconds $(date +%s)"
}
on_exit() {
    status=$?
    if [ "$status" -ne 0 ]; then
        on_failure
    fi
}
trap on_exit EXIT

mkdir -p "$BACKUP_DIR"
log "dumping database '$DB' from ${PGHOST:-localhost}"
pg_dump --format=custom --compress=6 --no-owner --no-privileges --dbname="$DB" --file="$PARTIAL"

log "verifying archive"
pg_restore --list "$PARTIAL" > /dev/null

mv "$PARTIAL" "$FINAL"
( cd "$BACKUP_DIR" && sha256sum "$(basename "$FINAL")" > "$(basename "$FINAL").sha256" )
SIZE="$(stat -c %s "$FINAL")"
log "wrote $FINAL ($SIZE bytes)"

if [ -n "${BACKUP_OFFSITE_CMD:-}" ]; then
    log "copying off-site"
    sh -c "$BACKUP_OFFSITE_CMD" -- "$FINAL"
fi

log "applying retention: delete dumps older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -maxdepth 1 -type f \( -name "$DB-*.dump" -o -name "$DB-*.dump.sha256" \) \
    -mtime +"$RETENTION_DAYS" -print -delete

NOW="$(date +%s)"
write_metric hotel_backup_success.prom "# HELP hotel_backup_last_success_timestamp_seconds Last successful backup (unix time)
# TYPE hotel_backup_last_success_timestamp_seconds gauge
hotel_backup_last_success_timestamp_seconds $NOW
# HELP hotel_backup_last_size_bytes Size of the last successful backup
# TYPE hotel_backup_last_size_bytes gauge
hotel_backup_last_size_bytes $SIZE"
log "done"
