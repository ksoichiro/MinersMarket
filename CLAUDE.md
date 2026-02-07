# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Miner's Market is a Minecraft mod where players compete to earn 10,000 gold by mining and selling ores to an NPC merchant. Built with **Architectury** for cross-platform support (Fabric + NeoForge), targeting Minecraft 1.21.1 with Java 21.

## Build Commands

```bash
# Standard build (default target: 1.21.1)
./gradlew build -Ptarget_mc_version=1.21.1

# Build all supported versions
./gradlew buildAll

# Platform-specific builds
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1

# Full release (clean + buildAll + collectJars → build/release/)
./gradlew release

# Collect built JARs into build/release/
./gradlew collectJars
```

Note: Tests are excluded from builds (`-x test`). The build system auto-downloads JDK 21 via Foojay toolchain resolver.

## Architecture

### Multi-Platform Module Structure

The project uses Architectury's pattern to share code between Fabric and NeoForge:

- **`common-shared/`** — Platform-independent shared code (no build.gradle, included as srcDir by common module). Base package: `com.minersmarket`
- **`common-1.21.1/`** — Version-specific common module (Architectury common). Includes `common-shared` sources via `srcDir`. Contains shared resources (lang files, assets, data).
- **`fabric-base/`** — Fabric platform base code (no build.gradle, included as srcDir by fabric module)
- **`fabric-1.21.1/`** — Fabric platform build module. Maps to Gradle project `:fabric`. Includes `fabric-base` sources.
- **`neoforge-base/`** — NeoForge platform base code (no build.gradle, included as srcDir by neoforge module)
- **`neoforge-1.21.1/`** — NeoForge platform build module. Maps to Gradle project `:neoforge`. Includes `neoforge-base` sources.

### Module Resolution in settings.gradle

Gradle project names differ from directory names. `settings.gradle` dynamically resolves modules based on `target_mc_version`:
- `:common-1.21.1` → `common-1.21.1/`
- `:fabric` → `fabric-1.21.1/`
- `:neoforge` → `neoforge-1.21.1/`

### Multi-Version Support

- Version properties in `props/<version>.properties` (currently only `1.21.1`)
- Override target version: `-Ptarget_mc_version=<version>`
- Version-specific task aliases: `build1_21_1`, `clean1_21_1`

### Where to Place Code

- **Shared game logic** → `common-shared/src/main/java/com/minersmarket/`
- **Version-specific common code** → `common-1.21.1/src/main/java/`
- **Fabric-specific code** → `fabric-base/src/main/java/com/minersmarket/fabric/`
- **NeoForge-specific code** → `neoforge-base/src/main/java/com/minersmarket/neoforge/`
- **Assets/resources** → `common-1.21.1/src/main/resources/`
- **Fabric resources** (fabric.mod.json) → `fabric-base/src/main/resources/`
- **NeoForge resources** (neoforge.mods.toml) → `neoforge-base/src/main/resources/META-INF/`

### Key Dependencies

- Architectury API 13.0.8 (cross-platform abstraction)
- Fabric Loader 0.17.3 / Fabric API 0.116.7+1.21.1
- NeoForge 21.1.209
- Mojang official mappings

## Implementation Plan

Detailed plan in `docs/plan.md` with 11 phases. MVP = Phases 1-10. Current status: infrastructure scaffolding complete, no Java source yet.

Package structure: `com.minersmarket.{registry,state,entity,block,item,trade,hud,network,event,structure}`
