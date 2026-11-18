# Custom Aura Files Implementation Plan

**Goal:** Import and share JSON aura surfaces and publish authoring instructions and source to the requested GitHub repository.
**Architecture:** Validated immutable common-side definitions travel with AuraSettings. Client-only file loading feeds a paged picker and custom mesh emitter; existing shader paths remain shared.
**Tech Stack:** Java 17, Forge 1.20.1, Gson, JUnit, Gradle.
**Spec:** ../specs/2026-11-18-custom-auras-design.md

## Constraints / review focus
- No executable scripts or unbounded geometry; reject malformed/oversized/deep data.
- Saved/received definitions do not require a local file or overwrite imported files.
- Changing color/limits/effects preserves a custom definition; built-in shape selection clears it.
- Shader and dedicated server paths must not acquire client-only dependencies.
- Publish an explicit allowlist of source assets, never local game files or credentials.

## Tasks
- [x] Definition schema/parser, samples, invalid input tests.
- [x] Settings, codec/config persistence, command copy, round-trip tests.
- [x] Custom mesh, bounds/framing, paged picker and reload diagnostics.
- [x] Runtime smoke test, final build, authoring docs and source publication.
