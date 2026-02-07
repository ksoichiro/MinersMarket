# Third-Party Licenses

This document lists all third-party dependencies used in Miner's Market and their respective licenses.

## Runtime Dependencies

### Minecraft & Core Framework

#### Minecraft
- **Project**: Minecraft Java Edition
- **Version**: 1.21.1
- **Developer**: Mojang Studios
- **License**: Minecraft EULA
- **URL**: https://www.minecraft.net/

#### Fabric Loader (Fabric)
- **Project**: Fabric Loader
- **Version**: 0.17.3+
- **Organization**: FabricMC
- **License**: Apache License 2.0
- **URL**: https://github.com/FabricMC/fabric-loader
- **License URL**: https://github.com/FabricMC/fabric-loader/blob/master/LICENSE

#### Fabric API (Fabric)
- **Project**: Fabric API
- **Version**: 0.116.7+1.21.1
- **Organization**: FabricMC
- **License**: Apache License 2.0
- **URL**: https://github.com/FabricMC/fabric
- **License URL**: https://github.com/FabricMC/fabric/blob/master/LICENSE

#### NeoForge (NeoForge)
- **Project**: NeoForge
- **Version**: 21.1.209+
- **Organization**: NeoForged
- **License**: LGPL-2.1-only
- **URL**: https://github.com/neoforged/NeoForge
- **License URL**: https://github.com/neoforged/NeoForge/blob/1.21.x/LICENSE.txt
- **Note**: Projects using NeoForge's APIs are not required to be licensed under LGPL-2.1

### Multi-Loader Support

#### Architectury API
- **Project**: Architectury API
- **Version**: 13.0.8+
- **Organization**: Architectury
- **License**: LGPL-3.0-only
- **URL**: https://github.com/architectury/architectury-api
- **License URL**: https://github.com/architectury/architectury-api/blob/1.19.2/LICENSE.md

## Development Dependencies

### Build Tools

#### Architectury Loom
- **Project**: Architectury Loom (Gradle Plugin)
- **Version**: 1.11-SNAPSHOT
- **Organization**: Architectury
- **License**: MIT License
- **URL**: https://github.com/architectury/architectury-loom

#### Architectury Plugin
- **Project**: Architectury Plugin (Gradle Plugin)
- **Version**: 3.4-SNAPSHOT
- **Organization**: Architectury
- **License**: MIT License
- **URL**: https://github.com/architectury/architectury-plugin

#### Gradle Shadow Plugin
- **Project**: Gradle Shadow
- **Version**: 8.3.6
- **Organization**: GradleUp
- **License**: Apache License 2.0
- **URL**: https://github.com/GradleUp/shadow

## License Summaries

### Apache License 2.0
Permissive license that allows commercial use, modification, distribution, and private use. Requires preservation of copyright and license notices.

**Used by**: Fabric Loader, Fabric API, Gradle Shadow Plugin

### LGPL-2.1-only
Copyleft license that requires derivative works to be licensed under LGPL-2.1. However, linking to libraries licensed under LGPL-2.1 does not require the linking code to be licensed under LGPL-2.1.

**Used by**: NeoForge

### LGPL-3.0-only
Similar to LGPL-2.1 but with updated terms for modern software development. Allows linking without requiring the linking code to adopt the same license.

**Used by**: Architectury API

### MIT License
Very permissive license allowing nearly unrestricted use, modification, and distribution. Only requires preservation of copyright and license notices.

**Used by**: Architectury Loom, Architectury Plugin

## Notes

- All dependencies are used in compliance with their respective licenses
- Runtime dependencies are not bundled with Miner's Market; users must install them separately
- NeoForge's LGPL-2.1 license does not affect Miner's Market's license due to linking exception
- Architectury API's LGPL-3.0 license does not affect Miner's Market's license due to linking exception

## License Compliance

Miner's Market is licensed under the LGPL-3.0-only License. All dependencies are compatible with this license:
- **Permissive licenses** (Apache 2.0, MIT): Fully compatible
- **LGPL licenses** (LGPL-2.1, LGPL-3.0): Compatible due to dynamic linking (no license propagation)

For questions about licensing, please contact the project maintainer.

---

Last updated: 2026-02-07
