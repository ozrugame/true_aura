# True Aura 0.8.0 verification

## Environment and scope

- Minecraft 1.20.1, Forge 47.4.23, Java 17.0.19 (aarch64), macOS / Apple M5, OpenGL 4.1 Metal.
- Optional runtime: Oculus 1.8.0, Embeddium 0.3.31, Epic Fight 20.14.17.
- Isolated singleplayer test world: `run-shaders/saves/AuraShaderTest`, copied from the previous flat development world. Existing user instances were not modified.
- Shader files: Complementary Reimagined r5.9.3, Complementary Unbound r5.9.3, BSL v10.1.8. Downloaded from the official Modrinth project API/CDN and checked against its SHA-512 metadata in `.tools/shader-test/`.
- These are rendering smoke tests, not performance benchmarks or a guarantee for every pack, preset, GPU, or mod combination. The two-client multiplayer visual confirmation from 0.6.1 is historical and was not repeated with shaders for this release.

## Configurable limits

`AuraLimits` defaults to size 8 and intensity 8, with finite hard bounds of 32. Forge SERVER config sets per-world maxima and syncs to joining clients; the local CLIENT limits are used only without a True Aura server. Editor sliders, commands, outbound saved settings, and server-received edits respect the applicable limits. Protocol is now 4; modded server and clients must both update.

Runtime test world explicitly uses `maxScale = 6.0`, `maxIntensity = 7.0`. The test sets size 4 and intensity 5 successfully, then attempts size 7 and intensity 8. Both invalid commands report the configured maximum; screenshots and logged state remain at 4 / 5. The preview camera backs away to frame the larger aura. Ground-impact footprint and native particle scaling remain capped at an effective size of 2.

Edit a world config while the world/server is closed, then restart and reconnect. Live propagation of a changed server config to already connected clients is not claimed.

## Shader rendering fix

The original effect disappeared when Oculus composited its world framebuffer. Drawing after level rendering alone still failed because Forge 1.20.1 supplies a projection pose at `AFTER_LEVEL`. True Aura now captures the actual world view/normal matrices at `AFTER_PARTICLES`, restores them at `AFTER_LEVEL`, and draws its aura and cosmetic ground impact onto the main target after composition. Shadow passes are skipped. Without an active shader pack, the original Forge stages and Fabulous target path remain in use.

The optional bridge uses the public Iris API through reflection and does not introduce a required Oculus dependency. Shaders, test harness classes, and development mod dependencies must not be bundled in the release JAR.

Aura rendering is after shader post-processing: it does not contribute to the pack's bloom/reflections or inherit all its lighting. Water/glass ordering, unusual presets, other shader loaders, remote-player shader rendering, and prolonged gameplay are outside this visual check.

## Reproducible visual smoke test

`tools/shader-test/java/dev/trueaura/client/ShaderSmokeTest.java` is a separate development source set. It switches the three packs, then disables shaders, drives aura commands, captures screenshots with Minecraft's Screenshot API, and leaves the client open. It temporarily builds a stone wall in the isolated world and fixes the test camera beyond it to verify opaque depth occlusion. The wall is removed before proceeding.

```sh
./gradlew runClient -PauraEpicFight=true -PauraShaderTest=true \
  -PauraRunDir=run-shaders -PauraUser=AuraShaderTest \
  -Dhttps.protocols=TLSv1.2
```

Requires local Oculus/Embeddium test jars in `.tools/shader-test/mods` and the named shader ZIPs in `run-shaders/shaderpacks`. Do not enable this test property in an ordinary player world. The harness intentionally changes aura settings and a small test area.

Pack index: 0 Reimagined, 1 Unbound, 2 BSL, 3 shaders disabled. Evidence is in `run-shaders/screenshots/aura-shader-<index>-<stage>.png` and `/tmp/trueaura-080-shaders-occlusion.log`. Prior failed attempts are separated under `before-render-fix` / `before-camera-fix`.

Startup logs include third-party warnings about optional missing integrations, unsupported GLSL extension/unused shader attributes, Epic Fight's web connection, and an invalid pre-existing test simulation distance of 4 (vanilla minimum 5). These are recorded rather than treated as evidence of a clean log. No True Aura crash or shader fallback was observed in the completed three-pack camera-fix run.

## Automated verification and artifact

Visual inspection completed on 2026-11-17:

| Runtime | Pack fallback | Aura visible | Open/close ground impact | Size 4 / intensity 5 preview | Opaque wall hides aura | Aura gone after closing |
| --- | --- | --- | --- | --- | --- | --- |
| Reimagined r5.9.3 | false | Pass | Pass | Pass | Pass | Pass |
| Unbound r5.9.3 | false | Pass | Pass | Pass | Pass | Pass |
| BSL v10.1.8 | false | Pass | Pass | Pass | Pass | Pass |
| Oculus shaders disabled | false | Pass | Pass | Pass | Pass | Pass |

Each screenshot logs the effective settings and shader API state. All four runs rejected size 7 / intensity 8 against the custom 6 / 7 world limits. There were no `AURA_SHADER_TEST FAILED` entries. The isolated test client was then stopped for the final release build. The shaders-disabled case still had Oculus/Embeddium loaded; it is not an independent no-loader runtime test.

Final `./gradlew test --rerun build --console=plain -Dhttps.protocols=TLSv1.2` without the shader-test property completed successfully (exit 0). JUnit XML: **52 tests, 0 failures, 0 errors, 0 skipped**. Coverage includes finite bounds, configured clamping, preservation of unrelated settings, packet round-trips above 2, command argument limits, preview framing, animation/geometry, and server revision/queue behavior.

Release JAR metadata reports 0.8.0 and contains the new limits/config classes. Archive inspection found no ShaderSmokeTest class, Oculus/Embeddium/Iris implementation, nested mod JAR, or shader-pack ZIP. The reobfuscated artifact is `release/trueaura-0.8.0-forge-1.20.1.jar`.

SHA-256: `c8a683058a033cd80668bc701c4fdc24800af0062a77186b575800d7882e3b7e`. Checksums for this and historical releases are in `release/SHA256SUMS.txt`. Build log: `/tmp/trueaura-080-final-build.log`.
