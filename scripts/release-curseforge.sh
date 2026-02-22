#!/bin/bash

# --- Project-specific settings ---
MOD_ID="minersmarket"
CURSEFORGE_PROJECT_ID="1457302"
PROJECT_NAME="Miner's Market"
# ---------------------------------

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/build/release"
CURSEFORGE_API="https://minecraft.curseforge.com/api"

if [ -z "$CURSEFORGE_TOKEN" ]; then
  echo "Error: CURSEFORGE_TOKEN environment variable is not set." >&2
  echo "Generate a token at: https://www.curseforge.com/account/api-tokens" >&2
  exit 1
fi

if [ -z "$CURSEFORGE_PROJECT_ID" ]; then
  echo "Error: CURSEFORGE_PROJECT_ID is not configured in the script." >&2
  exit 1
fi

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

# Map loader name to CurseForge display name
case "$LOADER" in
  fabric)   CF_LOADER="Fabric" ;;
  neoforge) CF_LOADER="NeoForge" ;;
  forge)    CF_LOADER="Forge" ;;
  *)
    echo "Error: Unknown loader: $LOADER" >&2
    exit 1
    ;;
esac

# Fetch game version types to identify the correct Minecraft version category
# (needed to avoid Bukkit version ID conflicts)
echo "Fetching game version types..."
VERSION_TYPES=$(curl -s "$CURSEFORGE_API/game/version-types" \
  -H "X-Api-Token: $CURSEFORGE_TOKEN")

if [ -z "$VERSION_TYPES" ] || [ "$VERSION_TYPES" = "null" ]; then
  echo "Error: Failed to fetch version types from CurseForge API." >&2
  exit 1
fi

# Find the Minecraft version type ID (e.g., "Minecraft 1.21" for version "1.21.1")
MC_MAJOR="${GAME_VERSION%.*}"
MC_TYPE_ID=$(echo "$VERSION_TYPES" | jq --arg mc "Minecraft $MC_MAJOR" \
  '[.[] | select(.name == $mc)] | first | .id // empty')

if [ -z "$MC_TYPE_ID" ]; then
  echo "Error: Could not find version type for Minecraft $MC_MAJOR" >&2
  exit 1
fi

# Fetch game versions
echo "Fetching game versions..."
VERSIONS_JSON=$(curl -s "$CURSEFORGE_API/game/versions" \
  -H "X-Api-Token: $CURSEFORGE_TOKEN")

if [ -z "$VERSIONS_JSON" ] || [ "$VERSIONS_JSON" = "null" ]; then
  echo "Error: Failed to fetch game versions from CurseForge API." >&2
  exit 1
fi

# Resolve Minecraft version ID (filtered by type to avoid Bukkit duplicates)
GAME_VERSION_ID=$(echo "$VERSIONS_JSON" | jq --arg v "$GAME_VERSION" --argjson t "$MC_TYPE_ID" \
  '[.[] | select(.name == $v and .gameVersionTypeID == $t)] | first | .id // empty')

if [ -z "$GAME_VERSION_ID" ]; then
  echo "Error: Could not find CurseForge game version ID for $GAME_VERSION" >&2
  exit 1
fi

# Resolve loader version ID
LOADER_VERSION_ID=$(echo "$VERSIONS_JSON" | jq --arg l "$CF_LOADER" \
  '[.[] | select(.name == $l)] | first | .id // empty')

if [ -z "$LOADER_VERSION_ID" ]; then
  echo "Error: Could not find CurseForge version ID for loader $CF_LOADER" >&2
  exit 1
fi

echo "Game version ID: $GAME_VERSION_ID, Loader version ID: $LOADER_VERSION_ID"

# Read changelog: use changelog-{version}.md if exists, otherwise extract from CHANGELOG.md
CHANGELOG_FILE="$PROJECT_ROOT/changelog-${MOD_VERSION}.md"
if [ -f "$CHANGELOG_FILE" ]; then
  CHANGELOG=$(cat "$CHANGELOG_FILE")
elif [ -f "$PROJECT_ROOT/CHANGELOG.md" ]; then
  # Extract the section between "## [MOD_VERSION]" and the next "## [" heading
  CHANGELOG=$(awk -v ver="$MOD_VERSION" \
    '/^## \[/{if(found) exit; if(index($0, "## ["ver"]")) {found=1; next}} found{print}' \
    "$PROJECT_ROOT/CHANGELOG.md" \
    | awk 'NF{found=1} found' \
    | sed -e 's/[[:space:]]*$//')
else
  CHANGELOG=""
fi

# Build metadata JSON
METADATA=$(jq -nc \
  --arg changelog "$CHANGELOG" \
  --arg displayName "${PROJECT_NAME} ${MOD_VERSION}" \
  --argjson gameVersions "[$GAME_VERSION_ID, $LOADER_VERSION_ID]" \
  '{
    "changelog": $changelog,
    "changelogType": "markdown",
    "displayName": $displayName,
    "gameVersions": $gameVersions,
    "releaseType": "release"
  }')

echo "Metadata: $METADATA"

# Upload file
curl -i -X POST "$CURSEFORGE_API/projects/$CURSEFORGE_PROJECT_ID/upload-file" \
  -H "X-Api-Token: $CURSEFORGE_TOKEN" \
  -F "metadata=$METADATA" \
  -F "file=@${JAR_PATH}"
