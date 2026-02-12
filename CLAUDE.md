# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Miner's Market is a Minecraft mod where players compete to earn 10,000 gold by mining and selling ores to an NPC merchant. Built with **Architectury** for cross-platform support (Fabric + NeoForge/Forge), targeting Minecraft 1.21.4, 1.21.3, 1.21.1 (Java 21) and 1.20.1 (Java 17).

## Build Commands

```bash
# Standard build (default target: 1.21.1)
./gradlew build -Ptarget_mc_version=1.21.1

# Build for 1.20.1
./gradlew build -Ptarget_mc_version=1.20.1

# Build all supported versions
./gradlew buildAll

# Platform-specific builds
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1
./gradlew :forge:build -Ptarget_mc_version=1.20.1

# Full release (clean + buildAll + collectJars → build/release/)
./gradlew release

# Collect built JARs into build/release/
./gradlew collectJars
```

Note: Tests are excluded from builds (`-x test`). The build system auto-downloads the correct JDK via Foojay toolchain resolver (JDK 21 for 1.21.1+, JDK 17 for 1.20.1).

## Architecture

### Multi-Platform Module Structure

The project uses Architectury's pattern to share code between Fabric and NeoForge/Forge:

- **`common-shared/`** — Platform-independent shared code (no build.gradle, included as srcDir by common module). Base package: `com.minersmarket`
- **`common-1.21.1/`** — Version-specific common module for MC 1.21.1 (Architectury common). Includes `common-shared` sources via `srcDir`.
- **`common-1.21.4/`** — Version-specific common module for MC 1.21.4. Contains all sources (does NOT include `common-shared` via srcDir due to 1.21.2+ breaking API changes). Adds `assets/<namespace>/items/` for 1.21.4 item model system.
- **`common-1.21.3/`** — Version-specific common module for MC 1.21.3. Contains all sources (does NOT include `common-shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common-1.20.1/`** — Version-specific common module for MC 1.20.1. Includes `common-shared` sources via `srcDir`.
- **`fabric-base/`** — Fabric platform base code (no build.gradle, included as srcDir by fabric module)
- **`fabric-1.21.4/`** — Fabric platform build module for MC 1.21.4. Maps to Gradle project `:fabric`.
- **`fabric-1.21.3/`** — Fabric platform build module for MC 1.21.3. Maps to Gradle project `:fabric`.
- **`fabric-1.21.1/`** — Fabric platform build module for MC 1.21.1. Maps to Gradle project `:fabric`.
- **`fabric-1.20.1/`** — Fabric platform build module for MC 1.20.1. Maps to Gradle project `:fabric`.
- **`neoforge-base/`** — NeoForge platform base code (no build.gradle, included as srcDir by neoforge module)
- **`neoforge-1.21.4/`** — NeoForge platform build module for MC 1.21.4. Maps to Gradle project `:neoforge`.
- **`neoforge-1.21.3/`** — NeoForge platform build module for MC 1.21.3. Maps to Gradle project `:neoforge`.
- **`neoforge-1.21.1/`** — NeoForge platform build module. Maps to Gradle project `:neoforge`.
- **`forge-base/`** — Forge platform base code (no build.gradle, included as srcDir by forge module)
- **`forge-1.20.1/`** — Forge platform build module for MC 1.20.1. Maps to Gradle project `:forge`.

### Module Resolution in settings.gradle

Gradle project names differ from directory names. `settings.gradle` dynamically resolves modules based on `target_mc_version`:
- For 1.21.4: `:common-1.21.4` → `common-1.21.4/`, `:fabric` → `fabric-1.21.4/`, `:neoforge` → `neoforge-1.21.4/`
- For 1.21.3: `:common-1.21.3` → `common-1.21.3/`, `:fabric` → `fabric-1.21.3/`, `:neoforge` → `neoforge-1.21.3/`
- For 1.21.1: `:common-1.21.1` → `common-1.21.1/`, `:fabric` → `fabric-1.21.1/`, `:neoforge` → `neoforge-1.21.1/`
- For 1.20.1: `:common-1.20.1` → `common-1.20.1/`, `:fabric` → `fabric-1.20.1/`, `:forge` → `forge-1.20.1/`

### Multi-Version Support

- Version properties in `props/<version>.properties` (`1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`)
- Override target version: `-Ptarget_mc_version=<version>`
- Version-specific task aliases: `build1_20_1`, `build1_21_1`, `build1_21_3`, `build1_21_4`, etc.
- 1.21.4, 1.21.3, and 1.21.1 use NeoForge; 1.20.1 uses Forge (NeoForge didn't exist for 1.20.1)

### Where to Place Code

- **Shared game logic** → `common-shared/src/main/java/com/minersmarket/`
- **Version-specific common code** → `common-<version>/src/main/java/`
- **Fabric-specific code** → `fabric-base/src/main/java/com/minersmarket/fabric/`
- **NeoForge-specific code** → `neoforge-base/src/main/java/com/minersmarket/neoforge/`
- **Forge-specific code** → `forge-base/src/main/java/com/minersmarket/forge/`
- **Assets/resources** → `common-<version>/src/main/resources/`
- **Fabric resources** (fabric.mod.json) → `fabric-base/src/main/resources/` (version override in `fabric-<version>/src/main/resources/`)
- **NeoForge resources** (neoforge.mods.toml) → `neoforge-base/src/main/resources/META-INF/`
- **Forge resources** (mods.toml) → `forge-base/src/main/resources/META-INF/`

### Key Dependencies

**1.21.4**: Architectury API 15.0.3, Fabric Loader 0.18.4 / Fabric API 0.119.3+1.21.4, NeoForge 21.4.156
**1.21.3**: Architectury API 14.0.4, Fabric Loader 0.16.10 / Fabric API 0.107.3+1.21.3, NeoForge 21.3.95
**1.21.1**: Architectury API 13.0.8, Fabric Loader 0.17.3 / Fabric API 0.116.7+1.21.1, NeoForge 21.1.209
**1.20.1**: Architectury API 9.2.14, Fabric Loader 0.16.10 / Fabric API 0.92.2+1.20.1, Forge 47.3.0
- Mojang official mappings

### Platform-Specific gradle.properties

Each version-specific platform module requires a `gradle.properties` with `loom.platform`:
- `fabric-1.21.4/gradle.properties` → `loom.platform=fabric`
- `fabric-1.21.3/gradle.properties` → `loom.platform=fabric`
- `fabric-1.21.1/gradle.properties` → `loom.platform=fabric`
- `fabric-1.20.1/gradle.properties` → `loom.platform=fabric`
- `neoforge-1.21.4/gradle.properties` → `loom.platform=neoforge`
- `neoforge-1.21.3/gradle.properties` → `loom.platform=neoforge`
- `neoforge-1.21.1/gradle.properties` → `loom.platform=neoforge`
- `forge-1.20.1/gradle.properties` → `loom.platform=forge`

Without this, Architectury Loom does not create platform-specific dependency configurations (e.g., `neoForge`, `forge`).

## Implementation Plan

Detailed plan in `docs/plan.md` with 11 phases. MVP = Phases 1-10. Current status: Phase 1 complete.

Package structure: `com.minersmarket.{registry,state,entity,block,item,trade,hud,network,event,structure}`
