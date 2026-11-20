#!/bin/sh
# =============================================================================
# Restore drill (monthly, RTO <= 4h target): proves the latest backup can be
# restored. Restores into a throw-away database on the same server, checks it,
# and drops it again. Never touches the live database.
#
# Usage: restore-drill.sh [dump-file]     (default: newest dump in BACKUP_DIR)
# Env:   PGHOST PGPORT PGUSER PGPASSWORD, PGDATABASE (source db name, for the
#        default dump name), BACKUP_DIR (/backups), KEEP_DRILL_DB=1 to keep it.
# Exit:  0 = restore verified, non-zero = drill failed (treat as an incident).
# =============================================================================
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
DB="${PGDATABASE:-hotel}"
DUMP="${1:-}"
if [ -z "$DUMP" ]; then
    # Newest first; names embed a sortable UTC timestamp.
    DUMP="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name "$DB-*.dump" | sort -r | head -n 1)"
fi
if [ -z "$DUMP" ] || [ ! -f "$DUMP" ]; then
    echo "[restore-drill] no dump found in $BACKUP_DIR" >&2
    exit 2
fi

if [ -f "$DUMP.sha256" ]; then
    ( cd "$(dirname "$DUMP")" && sha256sum -c "$(basename "$DUMP").sha256" )
fi

DRILL_DB="restore_drill_$(date -u +%Y%m%d%H%M%S)"
echo "[restore-drill] restoring $DUMP into $DRILL_DB"
psql --dbname=postgres -v ON_ERROR_STOP=1 -q -c "CREATE DATABASE $DRILL_DB"

cleanup() {
    if [ "${KEEP_DRILL_DB:-0}" != "1" ]; then
        psql --dbname=postgres -q -c "DROP DATABASE IF EXISTS $DRILL_DB" || true
    fi
}
trap cleanup EXIT

pg_restore --no-owner --no-privileges --exit-on-error --dbname="$DRILL_DB" "$DUMP"

TABLES="$(psql --dbname="$DRILL_DB" -tA -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")"
echo "[restore-drill] restored tables: $TABLES"
[ "$TABLES" -gt 0 ] || { echo "[restore-drill] restored database is empty" >&2; exit 3; }

# Application schema checks (skipped when the dump is not an application database).
HAS_FLYWAY="$(psql --dbname="$DRILL_DB" -tA -c "SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'")"
if [ "$HAS_FLYWAY" = "1" ]; then
    FAILED="$(psql --dbname="$DRILL_DB" -tA -c "SELECT count(*) FROM flyway_schema_history WHERE NOT success")"
    LATEST="$(psql --dbname="$DRILL_DB" -tA -c "SELECT max(version::int) FROM flyway_schema_history WHERE success AND version IS NOT NULL")"
    echo "[restore-drill] flyway latest version: $LATEST, failed migrations: $FAILED"
    [ "$FAILED" = "0" ] || { echo "[restore-drill] failed migrations in backup" >&2; exit 4; }
    for t in users rooms bookings payments; do
        COUNT="$(psql --dbname="$DRILL_DB" -tA -c "SELECT count(*) FROM $t")"
        echo "[restore-drill] $t: $COUNT rows"
    done
fi
echo "[restore-drill] OK"
