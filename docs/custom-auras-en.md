# Custom aura files (0.9)

Place UTF-8 `.json` files in your instance's `config/trueaura/auras/`. In-game: **O → Aura → Shape → Open folder / Reload files**, page through the list, select an entry and Save. Three editable examples are created on first opening. [Examples](../examples/auras) and the [complete Thai field guide](custom-auras-th.md) are included.

```json
{
  "version": 1,
  "id": "my_pack:flame",
  "name": "My Flame",
  "pattern": "flame",
  "layers": [{
    "height": 1.5, "offset": 0,
    "profile": [[0,0.35],[0.5,0.8],[1,0.1]],
    "amplitude": 0.12, "waves": 6, "speed": 3, "twist": 0.3, "role": 0
  }]
}
```

Each layer is a radial surface around the player. Profile pairs are `[normalized height, radius in blocks]`; 2–12 points must increase strictly from height 0 to 1. Radii: 0–2. Height: 0.1–3 times player height (default 1.4). Offset: 0–2 blocks (default 0). Amplitude: 0–0.5 (default 0.08), waves: integer 1–16 (default 5), speed: −8 to 8 (default 3), twist: −3 to 3 turns bottom-to-top (default 0). Role: integer 0 main surface, 1 strands, 2 base energy (default 0). Player size multiplies all dimensions; colors/intensity remain player settings.

Required root fields: `version` (1), `id` (namespace:name, lowercase a–z/digits/underscores/hyphens, max 96 characters), `name` (max 128 UTF-8 bytes without control/format codes), `pattern` (one of the ten built-in names), `layers` (1–4). Each layer requires `profile`; other layer fields have defaults. Patterns affect transparency and motion, so geometry is not necessarily a fully opaque shell.

Maximum file and expanded definition size: 8192 UTF-8 bytes. Maximum 128 files per folder. Strict JSON: no comments, unknown/duplicate fields, nonfinite numbers, symbolic links or nested subfolders. Duplicate IDs are rejected after the first filename-sorted entry. Hover Reload for errors, or read the chat report after reloading. Valid files still load when another is invalid.

Edit the selected file and Reload to update the draft; Save publishes it. The selected validated definition is embedded in client config, so removing its source does not remove the saved selection. Invalid replacements leave the saved/draft definition intact. Built-in shape selection clears the imported definition.

Protocol 5 requires 0.9 on all modded peers. The server relays the bounded definition; viewers need no local file. No script or URL is executed. This is a procedural surface format, not an OBJ/Blockbench/GLSL importer. `/aura set copy_shape Steve` copies an online player's shape to yourself; `/aura player Alex set copy_shape Steve` requires OP 2 and applies only the shape, preserving the target's colors/intensity. Remote definitions are never installed in the local imports folder.

The existing Oculus compatibility rendering path also renders custom surfaces; post-processing integration limitations from 0.8 still apply. Editing Java or shaders requires rebuilding with JDK 17 and `./gradlew test build`.
