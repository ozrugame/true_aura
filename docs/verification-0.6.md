# True Aura 0.6.0 verification

## Scope

- `/aura` and `/aura self`: on, off, toggle, status, reset, set color main/highlight/base, intensity, size, speed, shape, open_effect, close_effect.
- `/aura player <targets>`: same actions, permission level 2, Minecraft online-player argument (names/selectors). Works from player, console or command block sources.
- Tab suggestions, numeric bounds, color names/hex, shared editor/command validation for registry particles and arguments.
- Commands apply immediately on server and broadcast to viewers; targeted modded owner saves appearance locally, updates enabled state, and refreshes any open editor draft.
- Network protocol 3. Command revisions supersede queued/in-flight edits. Duplicate command snapshots must not overwrite later local R/O changes. Normal owner echoes remain ignored.
- On/off is session state and retains previous respawn/dimension reset behavior. Appearance persists on modded owner clients; unmodded owners retain only server-session settings. Server has no new disk profile store.

## Automated verification

- Initial `test build`: 40 tests passed before command-tree tests were added.
- Command-tree tests check permission 0 self access, permission 2 named-target access, permission 1 denial, color parsing, numeric bounds, and ten shape suggestions.
- Selector parsing in a plain JVM initially failed because Minecraft registries were not bootstrapped. That test now covers named players; actual selectors remain a game-runtime check, using the standard EntityArgument.players parser.
- Server-state tests cover preserving queued appearance when toggling by command, rejecting stale client edits, accepting subsequent acknowledged local edits, command composition, player isolation and session cleanup.
- Revision codec tests cover round trips, invalid lengths and negative revision values.
- Fresh read-only review found one important duplicate-revision race. Reproduced with `duplicateCommandCannotUndoLocalToggleAfterAcknowledgement`: failed before the fix (local OFF overwritten by duplicate ON). Fixed by accepting only newer command revisions; full final build result recorded below.

## Runtime checks still needed

- In game: `/aura on`, `/aura set color main blue`, `/aura set shape lotus`, `/aura off`; confirm appearance, transitions and O values.
- With a second client: OP changes target while target edits O; confirm both see command result and target can subsequently change settings normally.
- Non-OP self commands work with cheats disabled; target branch denied. OP/console/command block selectors `@a`, `@p`, filtered selectors work against online players.
- Dedicated server launch and multiplayer commands have not been manually verified. Existing automated coverage and common-side import review do not replace these runtime checks.
- Existing Epic Fight particle support was user-confirmed in 0.5; command-triggered Epic Fight transitions still need user confirmation in this release.

## Final build result

`./gradlew test build --console=plain -Dhttps.protocols=TLSv1.2` completed successfully (exit 0). 47 tests, 0 failures/errors/skips. Log: `/tmp/trueaura-060-final-build.log`. Release JAR and SHA256SUMS updated. Duplicate-revision regression passes after failing before the fix.

`runClient -PauraEpicFight=true` launched the 0.6.0 development client; sound engine and resource/particle atlases finished loading. Log: `/tmp/trueaura-060-client.log` (session 19232). No True Aura startup error observed; existing optional Epic Fight WaveyCapes-layer warnings remain. Game command execution has not yet been user-confirmed. Release archive metadata says 0.6.0, contains the command and common particle classes, has matching Thai/English translation keys, and all release checksums verify.

## Follow-up multiplayer verification

See [0.6.1 localhost multiplayer results](verification-0.6.1-multiplayer.md): dedicated startup fixed, two real clients joined simultaneously, command settings synchronized, and user confirmed correct remote aura rendering in both directions.
