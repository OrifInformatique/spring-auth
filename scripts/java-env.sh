#!/usr/bin/env bash
# JDK 21 + Maven 3.9 in Docker. No Java required on the host.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose --profile workspace)

if [[ ! -f .env ]]; then
  cp env-dist .env
  echo "Fichier .env cree depuis env-dist (valeurs locales, a adapter si besoin)."
fi

ensure_up() {
  "${COMPOSE[@]}" up -d db java
}

cmd="${1:-up}"
if [[ $# -gt 0 ]]; then
  shift
fi

case "$cmd" in
  up)
    ensure_up
    echo
    "${COMPOSE[@]}" exec java java -version
    echo
    "${COMPOSE[@]}" exec java mvn -v
    echo
    echo "Pret. Exemples :"
    echo "  $0 mvn test-compile"
    echo "  $0 mvn -DskipTests package"
    echo "  $0 mvn -Dspring.profiles.active=test verify"
    echo "  $0 shell"
    ;;
  down)
    "${COMPOSE[@]}" down
    ;;
  shell)
    ensure_up
    "${COMPOSE[@]}" exec java bash
    ;;
  mvn)
    ensure_up
    "${COMPOSE[@]}" exec java mvn "$@"
    ;;
  exec)
    ensure_up
    "${COMPOSE[@]}" exec java "$@"
    ;;
  *)
    echo "Usage: $0 [up|down|shell|mvn <args>|exec <cmd>]" >&2
    exit 1
    ;;
esac
