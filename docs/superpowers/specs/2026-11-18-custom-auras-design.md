# Imported aura definitions

User request: import custom aura files and publish the code and creation guide to ozrugame/true_aura. Existing multiplayer customization must remain shared.

Implementation choice: versioned JSON definitions, imported from config/trueaura/auras; no executable scripts. Alternatives considered: Java addon mods (requires rebuilding and matching installations), raw GLSL (platform/shader loader complexity), JSON surface profiles (editable with a text editor, bounded and transferable). Use JSON profiles with up to four animated radial surfaces and one of the existing shader patterns. This is a format for procedural aura surfaces, not a generic OBJ/Blockbench importer.

Each definition supplies id, display name, pattern, and layers. A layer supplies height, vertical offset, radius profile, twist, wave amplitude/count/speed, and existing shader layer role. Bounds limit geometry/network work. Three examples explain distinct shapes. Existing colors, intensity, size and transition effects remain independent.

The shape picker adds pagination, Open folder and Reload. Bad files produce visible diagnostics and do not discard valid files. Selection is a draft until Save. The selected validated definition is embedded in settings/config and sent through protocol 5; remote clients need no original file. Choosing a built-in shape clears the custom definition. Existing commands add a copy-from-player branch so operators can reuse a player's imported definition without requiring a client folder on a dedicated server.

Publish source, Gradle wrapper, examples, Thai/English guides and CI, excluding local worlds, caches, credentials, third-party jars and test screenshots. Verify JSON failure cases, meshes, persistence/codec, runtime selection/reload and multiplayer transport before release.
