#!/usr/bin/env bash
set -euo pipefail

# Build all modules in this repository.
#
# Usage:
#   ./scripts/build-all.sh            # build (with tests where present)
#   ./scripts/build-all.sh --skip-tests
#
# Notes:
# - Most modules target Java 8; Acore targets Java 17.
# - If `mvn` is not available locally, this script will try to use Docker (maven images).

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SKIP_TESTS=0
for arg in "$@"; do
  case "$arg" in
    --skip-tests)
      SKIP_TESTS=1
      ;;
    -h|--help)
      sed -n '1,120p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 2
      ;;
  esac
done

modules_java8=(
  "ItemManager"
  "Gem"
  "AcShop"
  "GemShop"
  "GuildManager"
  "MedalCabinet"
  "AtTackCraft-Core"
)
modules_java17=(
  "Acore"
)

run_maven() {
  local java_version="$1"; shift
  local pom_path="$1"; shift

  local mvn_args=( -B -ntp -f "$pom_path" clean install )
  if [[ "$SKIP_TESTS" == "1" ]]; then
    mvn_args+=( -Dmaven.test.skip=true )
  fi

  if command -v mvn >/dev/null 2>&1; then
    mvn "${mvn_args[@]}"
    return
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "Error: neither 'mvn' nor 'docker' is available in PATH." >&2
    echo "Install Maven (recommended) or Docker, then retry." >&2
    exit 1
  fi

  local image
  if [[ "$java_version" == "8" ]]; then
    image="maven:3.9.9-eclipse-temurin-8"
  else
    image="maven:3.9.9-eclipse-temurin-17"
  fi

  docker run --rm \
    -v "$ROOT_DIR:/workspace" \
    -w /workspace \
    "$image" \
    mvn "${mvn_args[@]}"
}

cd "$ROOT_DIR"

echo "==> Building Java 8 modules"
for module in "${modules_java8[@]}"; do
  echo "::group::Building ${module}"
  run_maven 8 "modules/${module}/pom.xml"
  echo "::endgroup::"
done

echo "==> Building Java 17 modules"
for module in "${modules_java17[@]}"; do
  echo "::group::Building ${module}"
  run_maven 17 "modules/${module}/pom.xml"
  echo "::endgroup::"
done

echo "Done. Artifacts are under modules/*/target/*.jar"