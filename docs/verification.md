# Verification — 2026-11-09

## Latest correction — rebuilt 14:33 Asia/Vientiane

The user subsequently reported that the effect looked solid and motionless while actively playing (not paused). The original visibility confirmation did not establish animation quality. Source now removes the continuous shell opacity, adds explicitly upward-travelling highlights, and increases tip displacement. A ninth regression checks visible tip movement over half a second; it failed against the prior geometry.

The user requested a rebuild after the earlier approval-service usage limit. `rebuild-and-run.command` successfully completed the compile, test, build and reobfuscation tasks, then launched the updated Minecraft client. All **9 tests passed** (4 mesh, 5 state). Startup logs reached completed resource/shader loading without a True Aura shader error. `build/libs` and `release` now contain the corrected jar. Visual confirmation of the revised motion is still pending; shader loading alone does not establish appearance.

## Build and tests

- JDK: Eclipse Temurin 17.0.19, macOS ARM64.
- Minecraft 1.20.1 / Forge 47.4.23 / ForgeGradle 6.0.54 / Gradle 8.12.1.
- `./gradlew test build -Dhttps.protocols=TLSv1.2`: successful, including `reobfJar`.
- 9 automated tests passed: fade in/out and bounds; mid-fade reversal; disconnect reset; clock behavior and partial-tick bounds; 20-minute continuity; explicit unload reset; finite bounded 3D geometry with fixed vertex budget; closed seam and animated/crouched geometry; visible tip displacement over half a second (some cases share a test method).
- Initial state/mesh tests failed against empty implementations. The later 20-minute continuity regression failed before its fix, then passed.
- 11,520 vertices per rendered frame; buffers reused, no particles/entities spawned or accumulated.
- Compiler deprecation warnings concern compatibility APIs (`ResourceLocation` constructor and `ModLoadingContext.get`) in Forge 47.4.23. Build remains on Java 17 and Gradle 8; Gradle 9 compatibility is not claimed.

## Real game evidence

- Started a separate development client under this project's `run/` directory.
- Logs confirm Forge and True Aura loaded, custom shader registration did not fail, an integrated world started, and AuraDev entered it.
- User explicitly confirmed seeing the aura after being asked to try R and F5.
- The UI automation tool could not bind the Java window. The assistant did not personally inspect a screenshot of the rendered aura; resemblance to the reference and frame rate are not yet visually measured.
- Client startup included ordinary first-run config creation, an EARLYDISPLAY timing warning, a vanilla shader sampler warning and unavailable development-account Realms authentication. None prevented world entry.

## Review corrections in the final jar

- Client level unload now resets the aura, including dimension changes.
- The animation clock uses double precision and is reduced at a shared trigonometric period after speed scaling, avoiding the old 20-minute phase jump.
- Fabulous uses sorted geometry, surface depth writes and separate-alpha blending for the vanilla premultiplied transparency compositor. Fast/Fancy retain additive rendering.
- Final corrections passed compilation and all 9 tests. The restarted development client contains the updated classes and shaders; their appearance has not yet been visually confirmed.

## Still to check visually

- Close match to the reference clip's shape, brightness and motion.
- All camera angles, movement/crouching, and hiding in first-person/unsupported poses.
- Fabulous with water/glass, opaque-wall occlusion, and graphics-mode changes.
- F3+T resource reload, world/dimension changes and config extremes in the final jar.
- Shader packs and other rendering mods, actual frame rate, and multiplayer join behavior.

## Release

`release/trueaura-0.1.0-forge-1.20.1.jar` is copied from the latest reobfuscated build output, including the visual correction. `release/SHA256SUMS.txt` records its hash. Do not use source/classes directories as the installed mod. Installation instructions are in README.md.
