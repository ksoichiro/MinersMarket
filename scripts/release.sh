#!/bin/bash

# --- Project-specific settings ---
MOD_ID="minersmarket"
MODRINTH_PROJECT_ID="6sCy4MMM"
PROJECT_NAME="Miner's Market"
# Modrinth project IDs for dependencies
DEP_ARCHITECTURY="lhGA9TYQ"
DEP_FABRIC_API="P7dR8mSH"
# ---------------------------------

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/build/release"

if [ $# != 1 ]; then
  echo "Usage: $0 jar_file_name"
  exit 1
fi

JAR_FILE="$1"
JAR_PATH="$RELEASE_DIR/$JAR_FILE"

if [ ! -f "$JAR_PATH" ]; then
  echo "Error: File not found: $JAR_PATH" >&2
  exit 1
fi

if [[ "$JAR_FILE" =~ ^${MOD_ID}-([0-9.]+)\+([0-9.]+)-([a-z]+)\.jar$ ]]; then
  MOD_VERSION="${BASH_REMATCH[1]}"
  GAME_VERSION="${BASH_REMATCH[2]}"
  LOADER="${BASH_REMATCH[3]}"
else
  echo "Error: Invalid jar file name: $JAR_FILE" >&2
  exit 1
fi

echo "Releasing $JAR_FILE (mod version: $MOD_VERSION, game version: $GAME_VERSION, loader: $LOADER)"

# Read changelog: use changelog-{version}.md if exists, otherwise extract from CHANGELOG.md
CHANGELOG_FILE="$PROJECT_ROOT/changelog-${MOD_VERSION}.md"
if [ -f "$CHANGELOG_FILE" ]; then
  CHANGELOG=$(jq -Rs . < "$CHANGELOG_FILE")
elif [ -f "$PROJECT_ROOT/CHANGELOG.md" ]; then
  # Extract the section between "## [MOD_VERSION]" and the next "## [" heading
  CHANGELOG=$(awk -v ver="$MOD_VERSION" \
    '/^## \[/{if(found) exit; if(index($0, "## ["ver"]")) {found=1; next}} found{print}' \
    "$PROJECT_ROOT/CHANGELOG.md" \
    | awk 'NF{found=1} found' \
    | jq -Rs 'sub("\n+$"; "")')
else
  CHANGELOG='""'
fi

if [ "$LOADER" = "fabric" ]; then
  DEPENDENCIES='[
      {"project_id": "'"$DEP_ARCHITECTURY"'", "dependency_type": "required"},
      {"project_id": "'"$DEP_FABRIC_API"'", "dependency_type": "required"}
    ]'
else
  DEPENDENCIES='[
      {"project_id": "'"$DEP_ARCHITECTURY"'", "dependency_type": "required"}
    ]'
fi

# Status needs to be "listed"
DATA=$(jq -c <<EOF
{
  "project_id": "${MODRINTH_PROJECT_ID}",
  "name": "${PROJECT_NAME} ${MOD_VERSION}",
  "version_number": "${MOD_VERSION}",
  "version_type": "release",
  "loaders": ["${LOADER}"],
  "game_versions": ["${GAME_VERSION}"],
  "status": "listed",
  "featured": false,
  "file_parts": ["file"],
  "changelog": ${CHANGELOG},
  "dependencies": ${DEPENDENCIES}
}
EOF
)

echo "Payload: $DATA"

# Check if the same version already exists
EXISTING=$(curl -s "https://api.modrinth.com/v2/project/${MODRINTH_PROJECT_ID}/version" \
  | jq --arg v "$MOD_VERSION" --arg l "$LOADER" --arg g "$GAME_VERSION" \
    '[.[] | select(.version_number == $v and (.loaders | index($l)) and (.game_versions | index($g)))] | length')

if [ "$EXISTING" -gt 0 ]; then
  echo "Error: Version ${MOD_VERSION} for ${LOADER} ${GAME_VERSION} already exists." >&2
  exit 1
fi

# Create a new version
curl -i -X POST "https://api.modrinth.com/v2/version" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data=$DATA" \
  -F "file=@${JAR_PATH}"
