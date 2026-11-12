# Vision Mod 0.7.0 — Performance controls (preview source)

Minecraft Java 1.21.11 / Fabric; Java 21.

## Changes in 0.7.0
- Added **Performance** page, reachable from Right Shift -> PERFORMANCE / FPS.
- Three reversible profiles: MAX FPS, BALANCED, VISUAL.
- Adjustable ore scan radius, number of blocks scanned per tick and maximum number of outlines.
- Fixed ore outline buffer replacement: results are swapped at the end of a scan, rather than cleared halfway through.
- Cosmetics are optional: limit render distance, disable remote cosmetics, or disable cosmetics entirely; remote fetch batches capped to 3 per scan.
- Toggle player hearts and particle filtering; switch between filtering and hiding all particles.
- Performance settings persisted to `config/visionmod/performance.properties` and included in exportable `.json` profiles.
- Retains Free Look, water/lava toggles, individual ore colors/selections and Configs page from 0.6.0.

## What MAX FPS actually does
Defaults when no prior performance settings exist: disables Vision Mod ore outlines, player hearts and cosmetics; enables the mod's all-particle filter. FullBright, water/lava visibility and Free Look remain available. You can enable each feature independently.

**Not a general-purpose FPS replacement:** these changes optimize Vision Mod's own work. They cannot fix server TPS, other mods' rendering, graphics driver limits, or heavy shaders. For systemic improvements evaluate compatible Sodium, Lithium, FerriteCore and EntityCulling versions separately.

## Build
Use `.github/workflows/build.yml` (Java 21). `fabric.mod.json` pulls `${version}` from Gradle.

## Verification
Source package prepared from the prior 0.6.0 archive. Syntax inspection is possible here, but a full Fabric Minecraft runtime build and real in-game FPS benchmark are still required. Do not treat this as a stable release JAR until tested.
