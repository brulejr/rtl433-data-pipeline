#!/usr/bin/env bash
set -euo pipefail

echo "[entrypoint] starting rtl433-data-pipeline"

# Make sure /usr/local/bin is on PATH but keep whatever the base image configured
export PATH="/usr/local/bin:${PATH:-/usr/bin:/bin}"

# Support *_FILE pattern (Docker secrets) and direct env vars.
while IFS='=' read -r env_name env_value; do
  case "$env_name" in
    *_FILE)
      target="${env_name%_FILE}"

      if [ -n "$env_value" ] && [ -f "$env_value" ]; then
        echo "[entrypoint] loading secret for $target from $env_value"
        export "$target"="$(<"$env_value")"
      else
        echo "[entrypoint] WARNING: secret file '$env_value' for $env_name not found" >&2
      fi
      ;;
  esac
done < <(env)

JAVA_OPTS="${JAVA_OPTS:-}"

if [[ -z "${APP_MAIN_CLASS:-}" ]]; then
  echo "[entrypoint] ERROR: APP_MAIN_CLASS is not set" >&2
  exit 1
fi

echo "[entrypoint] using APP_MAIN_CLASS=$APP_MAIN_CLASS"
echo "[entrypoint] executing application..."

exec java ${JAVA_OPTS} \
  -cp "/app/resources:/app/classes:/app/libs/*" \
  "$APP_MAIN_CLASS"
