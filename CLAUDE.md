# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Miner's Market is a Minecraft mod where players compete to earn 10,000 gold by mining and selling ores to an NPC merchant. Cross-platform (Fabric + NeoForge/Forge) using a per-platform plugin model (Fabric Loom for Fabric, ModDevGradle for NeoForge/Forge — selected by MC version in `settings.gradle`; no Architectury runtime API dependency), targeting Minecraft 1.21.11, 1.21.10, 1.21.9, 1.21.8, 1.21.7, 1.21.6, 1.21.5, 1.21.4, 1.21.3, 1.21.1 (Java 21), 1.20.1 (Java 17), and 26.1, 26.1.1, 26.1.2, 26.2 (Java 25).

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

# Publish collected JARs (tokens via MODRINTH_TOKEN / CURSEFORGE_TOKEN env vars)
./gradlew releaseModrinth      # or -Pjar=<file> for a single JAR
./gradlew releaseCurseForge    # or -Pjar=<file> for a single JAR
```

Note: Tests are excluded from builds (`-x test`). The build system auto-downloads the correct JDK via Foojay toolchain resolver (JDK 25 for 26.x, JDK 21 for 1.21.1+, JDK 17 for 1.20.1). `buildAll` includes the 26.x versions. Gradle 9.4.0 / foojay-resolver 1.0.0.

The `releaseModrinth` / `releaseCurseForge` tasks come from the `gradle/shared` git submodule ([minecraft-mod-gradle-scripts](https://github.com/ksoichiro/minecraft-mod-gradle-scripts)), applied in `build.gradle`. After a fresh clone, run `git submodule update --init` if the submodule is missing. Release IDs are configured in `gradle.properties` (`modrinth_project_id`, `curseforge_project_id`, `release_project_name`).

## Architecture

### Multi-Platform Module Structure

The project shares code between Fabric and NeoForge/Forge without an Architectury API runtime dependency. Each platform module applies its own plugin (Fabric Loom for Fabric, ModDevGradle `net.neoforged.moddev` for NeoForge, `net.neoforged.moddev.legacyforge` for Forge). The common module exposes its source/resource dirs through consumable `commonJava` / `commonResources` configurations (declared in an `artifacts {}` block); platform modules consume them via `source configurations.commonJava` and `from configurations.commonResources` (replacing the former Architectury shadow/transform approach). For 26.x, the **common** module uses `net.neoforged.moddev` with `neoFormVersion` (NeoForm), not Loom.

Module directories (NB: 26.1 has NeoForge only; 26.1.1/26.1.2/26.2 have Fabric + NeoForge):

- **`common/shared/`** — Platform-independent shared code (no build.gradle, included as srcDir by common module). Base package: `com.minersmarket`
- **`common/1.21.1/`** — Version-specific common module for MC 1.21.1. Includes `common/shared` sources via `srcDir`.
- **`common/1.21.11/`** — Version-specific common module for MC 1.21.11. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes). Uses `Identifier` instead of `ResourceLocation` (1.21.11 rename).
- **`common/1.21.10/`** — Version-specific common module for MC 1.21.10. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common/1.21.9/`** — Version-specific common module for MC 1.21.9. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common/1.21.8/`** — Version-specific common module for MC 1.21.8. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common/1.21.7/`** — Version-specific common module for MC 1.21.7. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common/1.21.6/`** — Version-specific common module for MC 1.21.6. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common/1.21.5/`** — Version-specific common module for MC 1.21.5. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes). NBT API uses `getCompoundOrEmpty()` and default-value overloads.
- **`common/1.21.4/`** — Version-specific common module for MC 1.21.4. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes). Adds `assets/<namespace>/items/` for 1.21.4 item model system.
- **`common/1.21.3/`** — Version-specific common module for MC 1.21.3. Contains all sources (does NOT include `common/shared` via srcDir due to 1.21.2+ breaking API changes).
- **`common/1.20.1/`** — Version-specific common module for MC 1.20.1. Includes `common/shared` sources via `srcDir`.
- **`common/26.1/`**, **`common/26.1.1/`**, **`common/26.1.2/`**, **`common/26.2/`** — Version-specific common modules for MC 26.x. Each contains all sources (does NOT include `common/shared`). Apply `net.neoforged.moddev` with `neoFormVersion` (NeoForm), not Loom.
- **`fabric/base/`** — Fabric platform base code (no build.gradle, included as srcDir by fabric module)
- **`fabric/1.21.11/`** — Fabric platform build module for MC 1.21.11. Maps to Gradle project `:fabric`.
- **`fabric/1.21.10/`** — Fabric platform build module for MC 1.21.10. Maps to Gradle project `:fabric`.
- **`fabric/1.21.9/`** — Fabric platform build module for MC 1.21.9. Maps to Gradle project `:fabric`.
- **`fabric/1.21.8/`** — Fabric platform build module for MC 1.21.8. Maps to Gradle project `:fabric`.
- **`fabric/1.21.7/`** — Fabric platform build module for MC 1.21.7. Maps to Gradle project `:fabric`.
- **`fabric/1.21.6/`** — Fabric platform build module for MC 1.21.6. Maps to Gradle project `:fabric`.
- **`fabric/1.21.5/`** — Fabric platform build module for MC 1.21.5. Maps to Gradle project `:fabric`.
- **`fabric/1.21.4/`** — Fabric platform build module for MC 1.21.4. Maps to Gradle project `:fabric`.
- **`fabric/1.21.3/`** — Fabric platform build module for MC 1.21.3. Maps to Gradle project `:fabric`.
- **`fabric/1.21.1/`** — Fabric platform build module for MC 1.21.1. Maps to Gradle project `:fabric`.
- **`fabric/1.20.1/`** — Fabric platform build module for MC 1.20.1. Maps to Gradle project `:fabric`.
- **`fabric/26.1.1/`** — Fabric platform build module for MC 26.1.1. Maps to Gradle project `:fabric`.
- **`fabric/26.1.2/`** — Fabric platform build module for MC 26.1.2. Maps to Gradle project `:fabric`.
- **`fabric/26.2/`** — Fabric platform build module for MC 26.2. Maps to Gradle project `:fabric`. (No `fabric/26.1/` — 26.1 is NeoForge only.)
- **`neoforge/base/`** — NeoForge platform base code (no build.gradle, included as srcDir by neoforge module)
- **`neoforge/1.21.11/`** — NeoForge platform build module for MC 1.21.11. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.10/`** — NeoForge platform build module for MC 1.21.10. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.9/`** — NeoForge platform build module for MC 1.21.9. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.8/`** — NeoForge platform build module for MC 1.21.8. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.7/`** — NeoForge platform build module for MC 1.21.7. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.6/`** — NeoForge platform build module for MC 1.21.6. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.5/`** — NeoForge platform build module for MC 1.21.5. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.4/`** — NeoForge platform build module for MC 1.21.4. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.3/`** — NeoForge platform build module for MC 1.21.3. Maps to Gradle project `:neoforge`.
- **`neoforge/1.21.1/`** — NeoForge platform build module. Maps to Gradle project `:neoforge`.
- **`neoforge/26.1/`** — NeoForge platform build module for MC 26.1. Maps to Gradle project `:neoforge`.
- **`neoforge/26.1.1/`** — NeoForge platform build module for MC 26.1.1. Maps to Gradle project `:neoforge`.
- **`neoforge/26.1.2/`** — NeoForge platform build module for MC 26.1.2. Maps to Gradle project `:neoforge`.
- **`neoforge/26.2/`** — NeoForge platform build module for MC 26.2. Maps to Gradle project `:neoforge`.
- **`forge/base/`** — Forge platform base code (no build.gradle, included as srcDir by forge module)
- **`forge/1.20.1/`** — Forge platform build module for MC 1.20.1. Maps to Gradle project `:forge`.

### Module Resolution in settings.gradle

Gradle project names differ from directory names. `settings.gradle` dynamically resolves modules based on `target_mc_version`, and its `pluginManagement` block also selects the Fabric Loom plugin id by MC version (`fabric-loom` for MC 1.x, `net.fabricmc.fabric-loom` for MC 26.x). The common module is always the single Gradle project `:common`, pointed at `common/<target_mc_version>/`. The set of platform projects is driven by `enabled_platforms` in `props/<version>.properties`; each enabled platform is included as `:<platform>` → `<platform>/<target_mc_version>/`. For example:
- `:common` → `common/<version>/` (every version)
- For 1.20.1: `:fabric` → `fabric/1.20.1/`, `:forge` → `forge/1.20.1/` (`enabled_platforms=fabric,forge`)
- For 1.21.1 – 1.21.11: `:fabric` → `fabric/<version>/`, `:neoforge` → `neoforge/<version>/`
- For 26.1: `:neoforge` → `neoforge/26.1/` only (NeoForge-only — no `:fabric`/`:forge`)
- For 26.1.1 / 26.1.2 / 26.2: `:fabric` → `fabric/<version>/`, `:neoforge` → `neoforge/<version>/`

### Multi-Version Support

- Version properties in `props/<version>.properties` (`1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`). Each declares `enabled_platforms` (e.g. `neoforge` for 26.1; `fabric,neoforge` for 26.1.1/26.1.2/26.2).
- Override target version: `-Ptarget_mc_version=<version>`
- Version-specific task aliases: `build1_20_1`, `build1_21_1`, `build1_21_3`, `build1_21_4`, `build1_21_5`, `build1_21_6`, `build1_21_7`, `build1_21_8`, `build1_21_9`, `build1_21_10`, `build1_21_11`, `build26_1`, `build26_1_1`, `build26_1_2`, `build26_2`, etc.
- 1.21.11, 1.21.10, 1.21.9, 1.21.8, 1.21.7, 1.21.6, 1.21.5, 1.21.4, 1.21.3, and 1.21.1 use NeoForge; 26.x uses NeoForge (26.1 NeoForge only; 26.1.1/26.1.2/26.2 Fabric + NeoForge); 1.20.1 uses Forge (NeoForge didn't exist for 1.20.1)

### Where to Place Code

- **Shared game logic** → `common/shared/src/main/java/com/minersmarket/`
- **Version-specific common code** → `common/<version>/src/main/java/`
- **Fabric-specific code** → `fabric/base/src/main/java/com/minersmarket/fabric/`
- **NeoForge-specific code** → `neoforge/base/src/main/java/com/minersmarket/neoforge/`
- **Forge-specific code** → `forge/base/src/main/java/com/minersmarket/forge/`
- **Assets/resources** → `common/<version>/src/main/resources/`
- **Fabric resources** (fabric.mod.json) → `fabric/base/src/main/resources/` (version override in `fabric/<version>/src/main/resources/`)
- **NeoForge resources** (neoforge.mods.toml) → `neoforge/base/src/main/resources/META-INF/`
- **Forge resources** (mods.toml) → `forge/base/src/main/resources/META-INF/`

Migration caveats (Java sources are otherwise unchanged):
- The common module's Fabric `@Environment`/`EnvType` usage was previously remapped by Architectury. The NeoForge/Forge modules now add `compileOnly "net.fabricmc:fabric-loader:..."` so these annotations resolve at compile time (CLASS-retention, ignored at runtime). Caveat: NeoForge no longer strips client-only classes server-side.
- night-config (TOML) is bundled into the Fabric jar via Loom `include(...)` (plus `implementation` for the compile classpath on 26.x); NeoForge/Forge get it from the platform at runtime, so they declare it `compileOnly`.

### Key Dependencies

**26.2**: Fabric Loader 0.18.5 / Fabric API 0.152.2+26.2, NeoForge 26.2.0.6-beta, NeoForm 26.2-1
**26.1.2**: Fabric Loader 0.18.5 / Fabric API 0.145.4+26.1.2, NeoForge 26.1.2.4-beta, NeoForm 26.1.2-1
**26.1.1**: Fabric Loader 0.18.5 / Fabric API 0.145.4+26.1.1, NeoForge 26.1.1.15-beta, NeoForm 26.1.1-1
**26.1**: Fabric Loader 0.18.5 / Fabric API 0.144.3+26.1 (NeoForge only), NeoForge 26.1.0.1-beta, NeoForm 26.1-1
**1.21.11**: Fabric Loader 0.18.4 / Fabric API 0.141.3+1.21.11, NeoForge 21.11.38-beta
**1.21.10**: Fabric Loader 0.18.4 / Fabric API 0.138.4+1.21.10, NeoForge 21.10.64
**1.21.9**: Fabric Loader 0.18.4 / Fabric API 0.134.1+1.21.9, NeoForge 21.9.16-beta
**1.21.8**: Fabric Loader 0.18.4 / Fabric API 0.136.0+1.21.8, NeoForge 21.8.52
**1.21.7**: Fabric Loader 0.18.4 / Fabric API 0.129.0+1.21.7, NeoForge 21.7.25-beta
**1.21.6**: Fabric Loader 0.18.4 / Fabric API 0.128.2+1.21.6, NeoForge 21.6.20-beta
**1.21.5**: Fabric Loader 0.18.4 / Fabric API 0.126.0+1.21.5, NeoForge 21.5.96
**1.21.4**: Fabric Loader 0.18.4 / Fabric API 0.119.3+1.21.4, NeoForge 21.4.156
**1.21.3**: Fabric Loader 0.16.10 / Fabric API 0.107.3+1.21.3, NeoForge 21.3.95
**1.21.1**: Fabric Loader 0.17.3 / Fabric API 0.116.7+1.21.1, NeoForge 21.1.209
**1.20.1**: Fabric Loader 0.16.10 / Fabric API 0.92.2+1.20.1, Forge 47.3.0
- Mojang official mappings
- Build-plugin versions in root `gradle.properties`: `fabric_loom_version`=1.13.6 (MC 1.x), `fabric_loom_26_version`=1.15.5 (MC 26.x), `moddevgradle_version`=2.0.141 (NeoForge + legacy Forge)

### Platform Selection

Each module's platform is set by the plugin id it applies (`fabric-loom` / `net.fabricmc.fabric-loom`, `net.neoforged.moddev`, or `net.neoforged.moddev.legacyforge`); the Fabric Loom id is chosen by MC version in `settings.gradle`'s `pluginManagement`. The old Architectury Loom requirement of a per-module `gradle.properties` with `loom.platform=<platform>` is obsolete under this model and is no longer used.

### NBT Structure Conversion (1.20.1)

The 1.21.1 structure files are converted to 1.20.1 format during the 1.20.1 build by a Groovy/Java converter (`buildSrc` compiles the shared Querz-NBT converter classes, srcDir'd from `gradle/shared/src/main/groovy`, dependency `com.github.Querz:NBT:6.1` via jitpack), and `common/1.20.1/build.gradle` registers a `convertNbt` task that runs `com.github.ksoichiro.mcmod.V1_21ToV1_20NbtConverter`. `buildSrc` is used deliberately: applying the shared mcmod Gradle plugin (or a per-module buildscript classpath dependency) conflicts with Fabric Loom's classloader, whereas `buildSrc` does not diverge any project's buildscript classloader and is therefore Loom-safe. The original standalone Python script (`scripts/convert_nbt_1_21_to_1_20.py`, mirrored at `gradle/shared/convert_nbt_1_21_to_1_20.py`) still exists and is kept as a manual/ad-hoc conversion tool (see `gradle/shared/README_NBT_CONVERSION.md`); it is not part of the automated build.

## Implementation Plan

Detailed plan in `docs/plan.md` with 11 phases. MVP = Phases 1-10. Current status: Phase 1 complete.

Package structure: `com.minersmarket.{registry,state,entity,block,item,trade,hud,network,event,structure}`
