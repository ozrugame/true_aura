# Aura Motion Implementation Plan

**Goal:** Configurable appearance/disappearance and movement-driven aura deformation.
**Architecture:** Common bounded AuraAnimation settings; tick-driven opacity and spring motion; shared vertex sink; Motion settings tab and local replay.
**Tech Stack:** Java 17, Forge 1.20.1, JUnit.
**Spec:** ../specs/2026-11-20-aura-motion-design.md

- [x] Tests for instant/fade/reversal, settings/packet preservation, spring stability/teleport/wrapped yaw/anchoring.
- [x] Implement settings, persistence, network protocol 6 and commands.
- [x] Implement tick-driven opacity/motion and shared mesh deformation.
- [x] Add Motion tab, duration controls, strength and local replay.
- [x] Review, runtime and multiplayer verification; document and package 0.10.0.

Review focus: no motion accumulated on pause, first-frame instant mode, cancel/reset semantics, no imported-profile loss, finite bounded deformation, local preview never changes multiplayer state.

Publication: push the verified source and publish the JAR/checksum after the matching GitHub CI run succeeds.
