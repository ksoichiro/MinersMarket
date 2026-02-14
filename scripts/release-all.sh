#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RELEASE_DIR="$(cd "$SCRIPT_DIR/../build/release" && pwd)"

for i in $(ls -1 -r "$RELEASE_DIR"/*.jar); do
  "$SCRIPT_DIR/release.sh" "$(basename "$i")"
done
