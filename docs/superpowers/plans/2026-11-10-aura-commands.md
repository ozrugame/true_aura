# Aura commands

Implement `/aura self <action>` for every player and `/aura player <targets> <action>` for permission level 2 (including console/command blocks). Actions: on/off/toggle/status/reset, and set color main/highlight/base, intensity, size, speed, shape, open_effect, close_effect. Provide Tab completion and strict validation, reuse particle validation from the editor.

Server changes broadcast immediately and are applied to the owner's saved config. A command revision prevents queued or in-flight stale client updates undoing commands. If an editor is open, refresh its draft to the authoritative settings. Ordinary echoes remain ignored. Settings remain client-persisted; on/off retains existing session/reset behavior. Players without the mod have server-session settings only. Protocol 3 requires matching True Aura releases on modded peers.

Verify queue/revision race behavior, command argument/permission tree where practical, packet round trips, then full Gradle test/build. Package 0.6.0 and launch if the old client has exited. Manual multiplayer/GUI checks must be reported as pending unless observed.

## Completion ledger

Implementation complete. Shared particle validation moved to common-side package. Full build: 47/47 tests passed. Final fresh-context review identified duplicate command replay; regression failed on old behavior and passes after revision gating. Actual selectors, command blocks, dedicated server and multiplayer visuals remain manual runtime checks.
