#!/bin/sh
set -eu

UPLOAD_DIR="${BARBERSHOP_UPLOAD_DIR:-${RAILWAY_VOLUME_MOUNT_PATH:-/app/uploads}}"
mkdir -p "$UPLOAD_DIR"
chown app:app "$UPLOAD_DIR"

# Keep compatibility with files uploaded before the public URL was flattened to /uploads/{file}.
if [ -d "$UPLOAD_DIR/services" ]; then
  chown app:app "$UPLOAD_DIR/services"
fi

exec su-exec app java -Duser.timezone=America/Bahia -jar /app/app.jar
