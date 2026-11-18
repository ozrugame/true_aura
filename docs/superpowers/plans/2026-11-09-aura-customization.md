# Aura Customization Implementation Plan

> **For agentic workers:** Use executing-plans for integration and dispatching-parallel-agents for independent networking and client presentation after the shared contract is defined.

**Goal:** In-game per-player aura editing with multiplayer replication.
**Architecture:** Immutable normalized AuraSettings shared by config, packets and rendering; server owns identities and session state. Client draft settings preview locally, commit atomically on Save, then sync through an optional Forge channel.
**Tech Stack:** Java 17, Forge 1.20.1/47.4.23, GLSL 150, JUnit 5.
**Spec:** ../specs/2026-11-09-aura-customization-design.md.

## Global Constraints
Preserve accepted Flame style, R toggle; O editor; main/highlight/base colors; intensity 0.1–2, scale 0.5–2, speed 0.1–3; Flame/Flow/Spikes. Server and clients use 0.2 for shared visibility. Optional local-only fallback. No client classes loaded on dedicated server. Preview stays local until Save.

## Review Focus
- Malformed settings, especially NaN/Infinity and invalid shape/color input, cannot crash or create unbounded effects.
- Rapid toggles/edits must converge to the final state, and packet identity always comes from sender.
- Join/leave/respawn/dimension transitions must not leave stale or missing remote effects.
- Cancel, resize and reopen must preserve/discard draft appropriately and restore camera.
- Multiple auras must not share colors through stale uniforms or affect the renderer's shared buffers.

### Task 1 — shared contracts
- [ ] Add tests then implement `dev.trueaura.aura.AuraSettings` record `(int mainColor, int highlightColor, int baseColor, double intensity, double scale, double speed, AuraShape shape)` with DEFAULT and normalized canonical constructor; `AuraShape` enum FLAME/FLOW/SPIKES and safe `fromId(int)`.
- [ ] Extend pure tests and quick-test source inclusion; run all tests.

### Task 2 — networking (independent ownership)
- [ ] Create only `network/**` and tests: AuraNetwork.initialize(), canSync(Connection), sendToServer(AuraSettings, boolean), setClientReceiver(BiConsumer<UUID, AuraSnapshot>); AuraSnapshot(settings, enabled), null snapshot means remove. No compile-time client dependencies.
- [ ] Register direction-specific fixed-size messages. Server uses sender identity, normalizes settings, coalesces updates at at most 5/sec, broadcasts only to channel-capable same-dimension clients. Join/tracking sends states, logout removes, respawn/dimension resets disabled.
- [ ] Tests for serialization and coalescing plus read-only review; parent wires common initialization.

### Task 3 — client config/editor/rendering
- [ ] Extend AuraConfig load/save for settings with backwards-compatible original fields.
- [ ] Add O mapping and mod Config extension. Track local draft separately from committed settings; a remote UUID map owns fade states without retaining players/worlds.
- [ ] Add responsive editor: Appearance and Colors tabs, shape cycle, sliders, RGB hex fields and palette choices, Save/Cancel/Reset; restore prior camera on removal. Game remains live.
- [ ] Add per-player shader color/shape uniforms and geometry presets. Preserve old mesh overload for Flame tests; add all-shape geometry tests.
- [ ] Render local and nearby visible remote players with independent uniforms, fading and local animation clocks. Client connect sends saved settings, pending sends are coalesced; clear/re-sync on dimension transitions.

### Task 4 — integration and release
- [ ] Update common init, metadata BOTH sides, version 0.2.0, configurable dev client directories/usernames and dedicated-server run configuration.
- [ ] Full test/build and client startup; dedicated classloading check without accepting an EULA on the user's behalf.
- [ ] Fresh final review; fix material findings and rerun necessary checks.
- [ ] Preserve 0.1, package 0.2, update README and verification with exact tested/untested scope.

## Rulings
User's explicit feature request and multiplayer answer authorize implementation; no repeated approval needed for routine controls/layout defaults. No git repository exists; progress is recorded in docs.
