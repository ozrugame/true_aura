# True Aura 0.3 verification

## Automated checks

- Forge 1.20.1-47.4.23 / Java 17: `test build` completed successfully (38 seconds, final build on November 9).
- 25 tests passed, zero failures, errors or skips. Includes stable preview projection across aspect ratios/body headings and collision shortening, six distinct animated geometries, packet round trips for all six shapes, toggle transition suppression, and finite burst lifetimes.
- Final release JAR is reobfuscated, version 0.3.0, and contains the burst shader files and the camera access transformer. SHA-256 recorded in `release/SHA256SUMS.txt`.
- Fresh code review found two important rendering issues; both were addressed: stable pitch-independent preview camera orbit, and ground effects rendered on the main target after opaque block entities, before translucent aura rendering.
- Camera access transformer targets the verified Forge 1.20.1 mapping `Camera.m_90584_(DDD)V` (`setPosition`). It changes the camera only, never the player's position or rotation.

## Runtime verification

- Initial pre-review-fix client reached resource loading without a True Aura shader error; it was closed at the title screen to apply final fixes.
- Final client launched successfully on November 10; Forge resource reload and atlas initialization completed without a True Aura shader/access-transformer error in `/tmp/trueaura-030-final-client.log`. User subsequently confirmed in-world that the character is clear of the settings panel and activation/deactivation effects are all visible.
- Prior user confirmation covers 0.2 editing and saving only, not these 0.3 changes.

## User visual confirmation

On November 10 the user confirmed: “เห็นครบและตัวละครไม่ถูกบังแล้ว” after testing O and F5 → R. This confirms the requested basic framing and visible toggle effects. It does not establish all terrain cases, Fabulous ordering, multiplayer timing or Epic Fight compatibility.

## Additional manual checks

1. In a world, open O after looking straight down/up. Verify the avatar is left of the panel. Close via Save, Cancel and Esc and check the previous camera mode is restored.
2. Cycle all six shapes; check motion, selected colors, size and saved settings.
3. Stand on solid ground, use F5 and press R. Activation should expand a wave with cracks/debris; deactivation should contract a base-colored wave. World blocks must remain unchanged.
4. Check slabs, stairs, ledges and jumping. No ground cracks/debris should spawn when airborne or on liquid. Check Fancy and Fabulous.
5. Two clients with 0.3 and a modded server: compare each player's own settings and toggle effects. Joining, changing color and repeated snapshots should not trigger extra impacts.
6. Test representative Epic Fight/combat/camera/shader mod packs. No direct Epic Fight integration or runtime compatibility claim is made.

## Known limits / deferred minor

- Main-target ground decals retain terrain depth. Airborne shockwave rings may sort approximately against glass/water in Fabulous mode.
- A wall immediately beside the player can force the preview camera close; this preserves framing but can crop a large aura. Test in open space for a full view.
- Effect manager eviction/retrigger/world-clear behavior is bounded in code but does not yet have isolated unit-test coverage. Transition and lifetime primitives are covered.
- Actual visual appearance, two-client timing and compatibility with other mods require in-game observation; build success does not establish them.
