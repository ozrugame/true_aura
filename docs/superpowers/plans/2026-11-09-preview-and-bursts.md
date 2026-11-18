# Preview framing, more shapes, and toggle effects (0.3)

User request: move the live character clear of the right settings panel, add aura shapes, and add activation/deactivation effects reminiscent of Epic Fight ground impacts.

Implementation choices: keep Forge 1.20.1; reframe the view only while the editor is open; append VORTEX, LIGHTNING and PULSE to existing shape IDs. Original cosmetic shockwave/crack/debris effects require no Epic Fight dependency and never modify world blocks. Activation expands, deactivation contracts. Colors and scale follow each player's committed aura. Existing enabled-state synchronization drives remote effects without a new packet format. Initial snapshots and preview edits must not trigger bursts. Ground visuals require nearby collision surfaces, including slabs, and skip air/liquids. Effects are bounded and cleared on world changes.

Steps:
1. Add regression coverage for framing, six distinct animated shapes and burst lifetime/retrigger behavior.
2. Implement temporary editor camera framing and the three shapes in mesh/shader/localization.
3. Implement bounded bursts, surface cracks, debris and client transition integration.
4. Run full build/tests, review, package 0.3, launch development client, document observed checks and manual checks still needed.

Verification: numerical projection/lifetime/geometry tests; Forge compile and reobfuscation; shader-load/startup log check. Human visual confirmation remains needed for composition and multiplayer/other combat mods.

Progress: planned. Workspace has no git repository; preserve previous release jars.

Implementation: steps 1–3 complete. Offline regression tests initially failed because the new framing/burst classes were missing. Full Forge build then passed. Corrected an initially reversed yaw convention using an independently projected vanilla Y(yaw+180) test: observed failure, then fixed positive yaw and elevation compensation. All six shapes round-trip through the existing codec. No packet-format change. Final review/build and game launch pending.

Final review: fresh reviewer found two important issues: particle-target depth hid ground effects in Fabulous, and angle-only framing failed for extreme view pitch. Fixed by rendering burst decals after opaque block entities on explicitly bound main target with color-only writes; stable level preview orbit now uses body heading alone, camera-only position via one Forge access transformer, and collision shortening. Orbit projection tests observed RED (missing orbit API) then replacement implemented. Rendering-stage correction verified against cached LevelRenderer/RenderStateShard source; live Fabulous visual test remains manual.

Decision: rings share the ground-decal main target to avoid hard depth holes. Airborne rings have approximate ordering relative to glass/water; cost is a brief ordering artifact. Minor deferred: manager eviction/retrigger/world-clear paths use bounded standard collections but do not yet have isolated unit coverage. No two-client or combat-mod visual session was available, so no compatibility or multiplayer runtime claim.

November 10 continuation: final artifact packaged as release/trueaura-0.3.0-forge-1.20.1.jar (66,078 bytes), shader/AT/version entries checked and checksums updated. Final test report: 25 passed, zero failures/errors/skips. Final development client successfully launched; resource loading finished with no True Aura shader or access error. User asked to confirm framing and activation/deactivation visuals. See docs/verification-0.3.md for manual checks and limits. Implementation, packaging and launch complete; human visual confirmation pending.

User acceptance on November 10: all requested visible effects appear and the character is no longer obscured by the settings panel. Basic in-world visual confirmation complete; additional compatibility/edge-case checks remain documented in verification-0.3.md.
