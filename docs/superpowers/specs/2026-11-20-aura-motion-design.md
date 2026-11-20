# Aura transitions and movement response

Requested: configurable instant vs gradual appearance and physical-looking ripples driven by character movement. Extend existing settings/network/imported-shape support.

Implementation choice: independent instant/fade options for on and off, retained fade durations 1–100 ticks (0.05–5s), default legacy 10 ticks. Add a Motion tab with both modes, durations, motion strength 0–2 and a local-only replay button. Save applies/publishes, Cancel discards. Preview replay does not change actual enabled state or emit network updates.

A bounded damped spring per visible player's animation state follows observed position deltas, vertical movement and wrapped body-yaw changes at 20 ticks/second. It bends the upper mesh opposite movement and propagates a small ripple; feet remain anchored. Strength zero gives original geometry. Teleports/nonfinite positions/reset/unload clear history. Frame interpolation is read-only; no render-rate-dependent integration. Imported and built-in meshes share the same final deformation sink and existing shader paths.

Alternatives: full cloth simulation is unnecessarily expensive for 32 procedural auras; a time-only sine cannot react to acceleration. Use a spring plus traveling ripple driven by observed velocity and turning. No collision/damage/world effects are added.

Store animation settings in client config and AuraSettings; protocol 6 carries them to observers, who derive motion from the replicated entity. Extend commands for modes, duration and motion strength. Release 0.10.0 requires all modded peers to update. Publish source/docs/artifact to the established repository after automated, in-game and multiplayer checks.

User refinement: gradual appearance must rise like flames from the feet. Add a third Rise mode (default), growing vertex height with angularly staggered tips and early bright ignition, rather than whole-body opacity alone. At full progress the original mesh is unchanged. Gradual closing reverses the growth; instant still snaps. Pure Fade remains available.
