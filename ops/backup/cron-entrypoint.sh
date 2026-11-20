#!/bin/sh
# Entrypoint of the "backup" service (postgres:16-alpine + busybox crond).
# busybox crond starts jobs with an empty environment, so the libpq/backup
# variables are captured once (shell-quoted by `export -p`) and sourced by jobs.
set -eu

export -p | grep -E "export (PG|BACKUP_|METRICS_DIR)" > /etc/backup.env
chmod 600 /etc/backup.env

BACKUP_SCHEDULE="${BACKUP_SCHEDULE:-15 2 * * *}"          # daily 02:15 (container TZ)
DRILL_SCHEDULE="${RESTORE_DRILL_SCHEDULE:-30 3 1 * *}"   # monthly, 1st at 03:30

cat > /etc/crontabs/root <<EOF
$BACKUP_SCHEDULE . /etc/backup.env && /bin/sh /ops/backup/pg-backup.sh >> /proc/1/fd/1 2>&1
$DRILL_SCHEDULE . /etc/backup.env && /bin/sh /ops/backup/restore-drill.sh >> /proc/1/fd/1 2>&1
EOF

echo "[backup] schedule: backup '$BACKUP_SCHEDULE', restore drill '$DRILL_SCHEDULE'"
if [ "${BACKUP_ON_START:-0}" = "1" ]; then
    /bin/sh /ops/backup/pg-backup.sh || echo "[backup] initial backup failed (alert will fire if it persists)"
fi
exec crond -f -l 8
