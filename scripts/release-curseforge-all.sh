#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RELEASE_DIR="$(cd "$SCRIPT_DIR/../build/release" && pwd)"

for i in $(ls -1 "$RELEASE_DIR"/*.jar); do
  "$SCRIPT_DIR/release-curseforge.sh" "$(basename "$i")"
done
