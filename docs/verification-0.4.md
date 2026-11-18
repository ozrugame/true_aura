# True Aura 0.4 verification — November 10, 2026

## Automated and artifact checks

- Final `./gradlew test build --console=plain -Dhttps.protocols=TLSv1.2` succeeded in 15 seconds with Java 17 / Forge 1.20.1-47.4.23.
- 33 tests passed; no failures, errors or skips. Includes independent on/off settings, legacy constructor defaults, preservation during appearance edits, bounded ID/arguments, protocol-v2 round trips, malformed UTF-8/text lengths, Epic Fight bit-encoded IDs/counts, and the Shriek delay regression.
- Release JAR: `release/trueaura-0.4.0-forge-1.20.1.jar`, 87,608 bytes, reobfuscated. Version, picker/effect classes and access transformer checked. Epic Fight classes/assets are not embedded. SHA-256 is in `release/SHA256SUMS.txt`.
- Fresh independent code review identified excessive Shriek delay bypassing particle lifetime. Fixed by validating 0..60 ticks before parsing/spawning; observed failing regression before implementation, then full suite passed.
- Editor-level 500ms Test cooldown also covers original bursts and missing-mod fallback. Native particles are allowed in their required medium (e.g. bubbles in water); the original True Aura fallback retains its water restriction.

## Epic Fight runtime

- Official Forge 1.20.1 Epic Fight 20.14.17 binary downloaded from the project's Modrinth release; SHA-512 matched published metadata. Provider contracts inspected in official 20.14.15.1 source and the supported 20.14.x behavior.
- Initial userdev startup failed on Epic Fight SRG mixin names. Fixed development-only Mixin refmap remapping using ForgeGradle's generated `createSrgToMcp/output.srg`.
- Retry loaded Epic Fight and True Aura, completed resources/particle atlas creation, and joined the development world as AuraDev. Log: `/tmp/trueaura-040-epic-client.log`.
- Epic Fight logs optional missing WaveyCapes layers (its own message labels these development-only) and failure to connect to its web server. Neither stopped world entry. No claim that every external particle was visually tested.
- Final small fixes (Shriek bound, universal Test cooldown, water eligibility) were packaged in 0.4 and carried into the combined 0.5 release. User confirmed closing the previous game; final runtime verification continues in verification-0.5.md.

## Manual validation still needed

1. Open O → Effects. Search `epicfight:` and confirm the registry catalog is populated. Set Ground Slam for activation and Neutralize or Air Burst for deactivation. Test each locally and save.
2. Press R in third person on solid ground and confirm independent choices. Reopen settings/restart the game to verify persistence. Changing colors/shape must retain both choices.
3. Test None/original, parameterized block/item/dust particles and a water particle underwater. Invalid arguments should disable Save and give feedback. Shriek delay above 60 must be rejected.
4. Cancel/Esc should discard unsaved choices. Selection alone must not trigger bursts; Test must remain local.
5. Two 0.4 clients and a 0.4 server: verify remote choices, absence of repeated effects on initial snapshots/settings edits, missing-source-mod fallback, and different dimensions/reconnect.
6. Test other modpacks/graphics settings; generic parameter parsing cannot establish arbitrary provider tick/render compatibility.

## Supported scope and limits

- Catalog lists installed particle registry entries. It does not enumerate every mod's bespoke renderer, gameplay ability or animation effect.
- Epic Fight standalone emitters receive explicit adapters. Swing/projectile trails remain visible but unavailable because they need live animation/projectile context. Laser remains unavailable because it requires a long beam target. Unknown Epic types are shown with an explanation.
- Direct spawns have count/lifetime limits; heavyweight Epic emitters have a shared 10-tick cooldown. Providers may emit their own children (e.g. Force Field End emits 320 dust particles and a flash). This native behavior is retained; the direct spawn budget is not a universal cap on mod-generated children.
- External provider render/tick failures cannot all be detected by argument validation or construction-time handling. Test unfamiliar particles before adopting them.
- Protocol 2 requires True Aura 0.4 on installed server/client endpoints. Unmodded endpoints retain local-only fallback. Old 0.2/0.3 modded endpoints are incompatible with the new packet schema.

## Sources used

- Epic Fight providers: https://github.com/Antikythera-Studios/epicfight/tree/1.20.1/src/main/java/yesman/epicfight/client/particle
- Official release: https://modrinth.com/mod/epic-fight/version/20.14.17-mc1.20.1-forge
- Mixin userdev remapping implementation: https://github.com/SpongePowered/MixinGradle/blob/master/src/main/groovy/org/spongepowered/asm/gradle/plugins/MixinExtension.groovy
