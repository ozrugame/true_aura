# True Aura 0.6.1: localhost multiplayer verification

Date: 2026-11-10. Minecraft 1.20.1, Forge 47.4.23, Epic Fight 20.14.17, Java 17, macOS ARM64.

## Result

A dedicated localhost server started successfully and two independent clients connected simultaneously:

| Client | Directory | Permission | Verified aura |
| --- | --- | --- | --- |
| AuraDev | run-dev | OP level 2 | Gold / flame |
| AuraViewer | run-viewer | Ordinary player | Aqua / lightning |

The server `list` output reported both players online. Both clients logged connection to a modded server. Server commands changed each player's main color and shape, selected Epic Fight ground_slam for activation and the built-in burst for deactivation, then enabled both using `aura player @a on`. Server status showed the expected different settings; both clients received owner updates and saved the settings. The user explicitly confirmed: “เห็นออร่าของอีกคนถูกต้องทั้งสองฝั่ง” (each client sees the other player's correct aura).

The user then closed both clients normally. A subsequent off/on command check had no online targets, so this run does NOT claim a completed off/on visual regression or live permission-denial test. The server remains available at 127.0.0.1:25565. Normal player/OP permission-tree tests are covered by the automated suite.

## Fix discovered by the live server test

The first dedicated launch exposed a True Aura initialization crash: `safeRunWhenOn` invoked its supplier-validation path in development, linking client Screen classes on DEDICATED_SERVER under Java 17. Replaced this with the deferred, dist-guarded `unsafeRunWhenOn` invocation. The server subsequently loaded mods, created its world and reached `Done`, then accepted both clients. This is a real runtime reproduction followed by a successful dedicated-server check, not a simulated class-loading unit test.

Version bumped to 0.6.1; protocol remains 3. Epic Fight's server userdev run now uses the same SRG-to-named mixin remapping as its client run.

`./gradlew test build ... -PauraEpicFight=true` succeeded: 47 tests, zero failures/errors/skips. Packaged `release/trueaura-0.6.1-forge-1.20.1.jar` and updated release checksums.

## Local setup

- Server directory: `run-server`; new flat creative world: `aura-multiplayer-test`.
- User read/accepted Minecraft EULA and changed `run-server/eula.txt` to true themselves before server launch.
- Bound to 127.0.0.1 only. Offline mode permits separate development usernames without separate authenticated sessions; not exposed to the LAN or Internet.
- Client settings/worlds in the original `run` directory were preserved; multiplayer clients use their own directories.
- `.tools/multiplayer/export-launches.gradle` resolves Forge launch arguments and skips JavaExec, avoiding long-lived Gradle locks when opening multiple JVMs.
- `.tools/multiplayer/launch.py server|dev|viewer` launches the prepared server or clients. Clients quick-connect to 127.0.0.1:25565 using distinct offline UUIDs.
- Launch metadata is machine-local and should be regenerated after dependency/build changes with `./gradlew runClient runServer -I .tools/multiplayer/export-launches.gradle -PauraEpicFight=true -Dhttps.protocols=TLSv1.2`.

Logs: `/tmp/trueaura-multiplayer-prepare.log`, `/tmp/trueaura-multiplayer-server.log`, `/tmp/trueaura-multiplayer-dev.log`, `/tmp/trueaura-multiplayer-viewer.log`. Expected optional Epic Fight web/compatibility-layer warnings appeared; they did not prevent multiplayer connection or the user-confirmed aura rendering.
