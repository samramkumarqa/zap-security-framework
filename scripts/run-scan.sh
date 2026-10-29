#!/usr/bin/env bash
# Starts the target app and ZAP in Docker, runs the security suite against them, then tears everything down.
# Usage: scripts/run-scan.sh [extra maven args]
set -euo pipefail
cd "$(dirname "$0")/.."

compose() { docker compose "$@"; }

cleanup() { echo "Stopping containers..."; compose down -v >/dev/null 2>&1 || true; }
trap cleanup EXIT

echo "Starting Juice Shop and ZAP..."
compose up -d --wait

echo "Waiting for the ZAP API on localhost:8080..."
for i in $(seq 1 60); do
  if curl -sf "http://localhost:8080/JSON/core/view/version/" >/dev/null; then break; fi
  sleep 2
  [ "$i" = "60" ] && { echo "ZAP did not come up"; exit 1; }
done

echo "Running the security scan..."
mvn -B test \
  -Dtarget.url="http://juice-shop:3000" \
  -Dzap.host=localhost -Dzap.port=8080 \
  "$@"
