# True Aura 0.2 — customization and multiplayer

## User-authorized scope
The user accepted the animated 0.1 aura and requested an in-game screen to let each person customize colors, intensity and shape. They explicitly chose multiplayer visibility: other players must see each person's selected aura. Continue on Forge 1.20.1. The decisions below fill in implementation details; they are assistant defaults, not user-specified preferences.

## Screen and saved preferences
- R keeps toggling the aura. O opens Aura Settings; both keys can be remapped. Also expose the screen from Forge's mod Config button.
- Three editable RGB/hex colors: outer/main energy, pale inner highlights, and foot/base energy. Defaults approximate the accepted gold/orange and violet appearance.
- Sliders: intensity 0.1–2.0, scale 0.5–2.0, speed 0.1–3.0; shape picker with Flame (existing style), Flow (smooth twisting envelope), Spikes (pointed flaring envelope). Shape affects real geometry and shader appearance.
- Editing previews locally in the world, with a temporary front-third-person camera if needed. The editor does not pause the world. Restore the original camera when closing. Opening the editor previews the aura even if the saved toggle is off; other players do not see draft values.
- Save persists the preferences to the existing client TOML and sends the new settings to the server; Cancel/Escape discards the draft. Reset restores defaults in the draft only. Invalid hex cannot be saved. Preferences survive restart; aura enabled state remains session-only and initially off.
- UI fits Minecraft's minimum supported GUI area (320×240), with separate Appearance and Colors pages if needed. English and Thai labels.

## Multiplayer behavior
- Install 0.2 on both server and clients to share effects. Without a modded server, the screen and local aura remain usable; show a short local-only status.
- Client sends only its settings and on/off flag; server derives identity from the connection. Clamp finite numeric values and colors at a shared data boundary. Fixed-size packets; never trust client-supplied player UUIDs.
- Server broadcasts committed updates only to clients that have the channel. Synchronize current states on join and when players start tracking each other; remove on logout/server stop. Toggle/save changes propagate, not every animation frame.
- Store appearance locally per installation and use the authenticated player identity during a session. No server disk database is required: each client resends its saved appearance on join. Respawn/dimension changes reset off, matching the existing mod behavior, and do not leave remote ghosts.
- Bound packet frequency, keep the final desired state when coalescing rapid changes, and bound rendering by visible/tracked players within a practical distance. Never send custom packets to clients without the channel.
- Each viewer animates remote auras locally. Rendering uses each player's saved colors/shape/strength, with per-player uniform flushes and normal wall occlusion. Own aura remains hidden in first person outside the editor. Existing invisible/spectator/unsupported-pose rules also apply to remote players.

## Validation and deliverable
- Tests for settings normalization/NaN handling, color parsing, shape IDs, packet round-trip and direction/identity behavior where feasible, server state coalescing, and all shapes' finite bounded animated geometry.
- Build reobfuscated 0.2 jar. Verify dedicated-server classloading and client screen/shader startup; document any multiplayer/visual steps not actually exercised.
- Preserve the 0.1 release jar. Deliver 0.2 with Thai installation/key/settings instructions and explicit server/client requirements.
