# True Aura 0.7.0 impact effects — 2026-11-15

Implemented stronger activation/deactivation visuals for `trueaura:burst` and `epicfight:ground_slam`. Native Epic Fight's particle alone emits terrain debris/smoke; its Meteor Slam fracture also animates ground block models. Inspected installed Epic Fight source archive (MeteorSlamSkill, LevelUtil, FractureBlockRenderer, GroundSlamParticle) and implemented an independent cosmetic tile animation. No Epic Fight attack/fracture methods or world block mutations are invoked.

- Expanding activation wave reaches most of its radius in four ticks; secondary shockwave and thicker branched cracks.
- Closing uses an inward wave, inward tile timing/tilt, and an outward secondary pulse.
- Up to 36 sampled solid-cube tile models per trigger, 192 rendered tiles per frame, 32 active effects. Tiles lift, tilt and settle; block changes invalidate cached tile models. Block entities, plants, fluids and partial blocks are excluded from tile copies. Cracks still raycast actual surfaces.
- Eight-tick per-player cooldown on custom fracture visuals. Native Epic Fight's global particle budget does not suppress another player's lightweight tile animation.
- Native ground-slam parameters increased to radius 1.6 × scale, density 32 on / 24 off, radial power 1.7 on / -1.1 off. Native child particles still have Epic Fight's own lifetimes (the emitter lifetime cap does not bound children).
- Local impact sound, stronger built-in debris, main render target binding before tile drawing. None/other selected particle effects preserve their choice. No screen shake added.
- Existing protocol 3 and aura settings format unchanged.

Validation: `./gradlew test build --console=plain -Dhttps.protocols=TLSv1.2` succeeded, 48 tests, zero failures/errors/skips. Added animation checks for fast wave arrival, opposing travel directions, finite/non-negative lift, and no residual tile offset after expiration; updated Epic Fight parameter checks.

User visual confirmation (2026-11-15): “เห็นแผ่นพื้นแตกและแรงกระแทกชัดขึ้นแล้ว” — the ground fracture and stronger impact are visible as intended. The response did not specify which of burst/ground_slam was selected. Remaining coverage: both effect selections individually, scales 0.5/1/2, slabs/edges/water/air, simultaneous nearby players, Fabulous graphics and shader interactions. Earlier 0.6.1 multiplayer aura synchronization was user-confirmed, but does not verify these new tile effects.

Runtime: launched with `runClient -PauraEpicFight=true`; resources loaded and AuraDev joined the existing single-player world successfully. Log: `/tmp/trueaura-070-client.log`. User subsequently confirmed that the rendered ground fracture and impact are clearly visible. Release checksums all verified.
