# True Aura 0.2 verification — 2026-11-09

## Build and automated checks
- Java 17, Minecraft 1.20.1, Forge 47.4.23, ForgeGradle 6.0.54, Gradle 8.12.1.
- `./gradlew test build -Dhttps.protocols=TLSv1.2` completed successfully, including reobfuscation.
- Integrated `test build runClient` repeated after final render-order integration; 21 tests, 0 failures, 0 errors.
- Coverage: saved settings normalization including NaN/infinity and RGB bounds; valid/incomplete color input; safe shape IDs; fade/toggle/reset/clock; distinct animated and bounded geometry for all three shapes; fixed-size packet round trips/removals/malformed lengths; latest-wins coalescing and rate limits.
- New settings/state/preset tests were observed failing before implementation; networking contributor also verified red/green codec and coalescing tests.
- Independent final review found no material correctness defects in sender identity, optional-channel guards, lifecycle snapshots, draft isolation, camera restoration, colors/shape uniforms or bounded rendering.

## Actual runtime evidence
- Updated client launched and loaded both aura shaders/resources without a True Aura shader error.
- Logs show version 0.2 opened the existing isolated test world and AuraDev joined its integrated server.
- Dedicated-server launch reached the vanilla EULA check and exited. The EULA was not accepted automatically. This is **not** a completed dedicated-server startup or proof of all server-side runtime paths.
- The user tested the O settings screen and explicitly confirmed that changing the aura and saving work. This confirms the basic editor/preview/Save path; it does not establish every preset, resize case, restart persistence or multiplayer visibility.

## Remaining manual validation
- Two actual clients seeing each other's selected colors/shape/strength, then join/leave/respawn/dimension transitions.
- Full dedicated-server start after its owner accepts the EULA.
- Screen layout in Thai/English at minimum GUI sizes, Save/Cancel/reopen persistence, restoration of first-/third-person camera.
- Multiple overlapping auras in Fabulous, transparent glass/water, and shader-pack compatibility.

## Implementation notes
- R toggles; O opens settings; Forge Config button is registered.
- Draft values never go to the server until Save. Server identity comes from the connection, not a client UUID. Settings are normalized before rendering or relaying.
- Updates are coalesced to at most five per second per sender, preserving the latest desired state.
- Optional channel allows local-only behavior against an unmodded server and does not send packets to unmodded clients.
- Visible candidates are limited to 32 nearest players within 64 blocks, then drawn far-to-near; distant meshes use reduced tessellation.
- Original 0.1 release is retained. Install only one True Aura jar per mods directory.
