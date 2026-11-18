# Effect picker and Epic Fight integration (0.4)

Use executing-plans inline; review the whole change once at the end. No git repository in this workspace.

User requirement: independently choose aura activation/deactivation effects from Minecraft and installed mods, emphasizing Epic Fight.

Design decisions: append a new Effects tab to the existing draft/save editor; searchable paged registry catalog sorted Epic Fight first; None and original True Aura burst remain choices. All registered particle IDs are discoverable. Parameterized particle types get supported defaults or an editable particle-argument field validated using their own deserializer. Epic Fight effects with special context need explicit adapters; unsafe animation/entity-only effects remain visible with an explanation instead of being blindly spawned. No generic entity spawning, damage, block destruction or gameplay abilities. Selecting a particle replaces the built-in burst. A Test button uses the unsaved draft locally, throttled. Existing saved settings migrate to the original burst for both transitions. Same-version server/client required for new wire protocol; missing source mod on a viewer falls back to the built-in burst.

1. [ ] Add pure effect selection value (bounded ID/arguments), settings preservation helpers, config and protocol v2. Test independent on/off choices, malformed/oversized payloads, default migration and appearance edits preserving choices.
2. [ ] Add registry catalog / safe emission adapters. Inspect Epic Fight 1.20.1 providers and use proper parameters. Cap spawn budgets and heavyweight emitter counts. Parameter validation must not spawn particles.
3. [ ] Add inline Effects page with search/filter, separate opening/closing selection, parameters, availability feedback and local Test. Preserve raw edits, preview camera and Save/Cancel lifecycle.
4. [ ] Build/test without Epic Fight; optional dev-runtime with official stable Epic Fight 20.14.17. Review, package 0.4, launch and document observed verification and limitations.

Files: aura/AuraEffect.java; AuraSettings.java; client/AuraConfig.java, AuraParticleCatalog.java, AuraParticleEffects.java, AuraSettingsScreen.java, AuraBurstEffects.java; network/AuraPacketCodec.java and AuraNetwork.java; language JSON; targeted tests; optional development runtime in build.gradle.

Progress: source inspection. Existing .3 accepted by user. Use original Epic Fight code via installed mod, not copied visual assets or gameplay handlers.

Progress: tasks 1–3 implemented. New tests initially failed for missing effect/adapters; full Forge test build now passes 32 tests. Protocol v2 uses fixed 814/831-byte payloads with explicit bounded UTF-8 lengths. Original 7-field settings constructor preserves legacy defaults. All appearance/color UI paths use preservation helpers. Epic Fight source20.14.15.1 and official release20.14.17 inspected/downloaded with published SHA512 verification. Optional dev runtime initially exposed missing SRG refmap remapping; configured Mixin's documented remapRefMap/refMapRemappingFile settings. Retry loaded Epic Fight/True Aura and completed resource initialization. Visual world tests not yet observed. Final review pending.

Final review: one Important finding, Shriek delay bypassing age/lifetime, fixed with 0..60 timing validation and RED→GREEN regression. Minor preview-throttle gap fixed with editor-level 500ms cooldown to fulfill the original Test contract. All 33 tests pass; release JAR packaged and checksums updated. Runtime with Epic Fight successfully entered world before those small fixes. Waiting for user to exit/save world before final client restart. Known native child emissions/flash accepted and documented; arbitrary mod-provider graphics/timing and two-client visuals remain unverified.

User confirmed Epic Fight Ground Slam is visible in the combined 0.5 runtime. Final 0.5 build/package/launch and basic visual acceptance complete; see verification-0.5.md.
