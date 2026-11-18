# True Aura 0.5 verification — November 10, 2026

## Completed

- Combined release includes the independent activation/deactivation effect picker from 0.4, the visual color editor and ten aura shapes.
- Final Forge 1.20.1-47.4.23 / Java 17 `test build` succeeded in 7 seconds. **36 tests passed**, zero failures/errors/skips.
- Color coverage: primary/secondary hue positions, palette corners, clamped drags, exact saved RGB → HSV → RGB round trips including white/black.
- Geometry coverage: all ten forms are distinct, animated and bounded; seam continuity verified at every height/layer for both near and distant meshes. Packet tests iterate all shape IDs.
- Fresh read-only review of the color/shape delta found missing narrator value feedback. Fixed narration to announce the selected color channel and changing hue/saturation/brightness. The reviewer found no other important defect in that delta.
- Packaged reobfuscated `release/trueaura-0.5.0-forge-1.20.1.jar`, 94,279 bytes. Verified version, picker class and English/Thai shape labels; no Epic Fight classes embedded. SHA-256 in `release/SHA256SUMS.txt`.
- Previous game exited successfully after the user confirmed saving/closing it. Latest client launched with optional Epic Fight 20.14.17, completed resources/shader loading and joined a world successfully. No True Aura shader/launch error observed in `/tmp/trueaura-050-client.log`; Epic Fight optional cape-layer and web-server messages did not prevent world entry. User confirmed that visual color selection is easier, updates the aura and saves correctly; Epic Fight Ground Slam is visible in-game.

## User confirmation

The user confirmed on November 10: “เลือกสีง่ายขึ้น เปลี่ยนและบันทึกได้” and “เห็นเอฟเฟกต์แล้ว” for Epic Fight Ground Slam. This validates the requested basic color-picker and Ground Slam interactions. It does not establish every particle, all ten forms, or multiplayer compatibility.

## Additional manual checks

- O → Colors: select Main/Highlight/Base, drag hue strip and color field, verify live preview and swatch. Test Save, Cancel/Esc, presets, resize and keyboard arrows.
- Aura page: cycle all ten forms, including Inferno/Lotus/Blade/Veil. Check normal and maximum size.
- Effects page: search `epicfight:ground_slam`, select activation, choose a different deactivation effect, Test then Save. Confirm both R transitions. Check None, original burst and parameterized vanilla/mod particles.
- Multi-client timing and arbitrary modpack compatibility remain unverified. Earlier 0.3 user acceptance does not establish these new interactions.

## Limits / minor deferred

- White/black retain their exact saved RGB color across tab changes/resize. The picker's latent hue/saturation is reconstructed from RGB on rebuild, so brightening/re-saturating afterward may start from red; choose hue again if needed.
- See `verification-0.4.md` for registry scope, disabled context-dependent Epic Fight particles, native child-emitter behavior and provider compatibility limits.
- Protocol remains 2. Use 0.5 on clients/server for all ten forms; 0.4 can exchange the settings but falls back to Flame for new shape IDs. Versions 0.2/0.3 use an incompatible protocol.

## 0.5.1 — direct shape selection

Replaced the cycle button with an inline two-column list showing all ten localized names. Current shape has a gold check; selecting a name updates only the draft shape and returns to the sliders. Back/Esc closes just the list; switching tabs also closes it. Save/Cancel and appearance/effect preservation retain existing behavior. All rows fit between panel offsets64 and176, back at184 and footer at208. No new tests were added for this reversible UI change; the existing 36-test suite and full Forge build passed. Release jar/version/language entries checked and SHA256 updated. Latest 0.5.1 client with Epic Fight completed resource/shader loading without a True Aura launch error (`/tmp/trueaura-051-client.log`). The name-list interaction itself awaits user visual confirmation.
