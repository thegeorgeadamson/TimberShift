# TimberShift 1.3.0 validation

Validated on 10 October 2026. Target compatibility: Minecraft Java 1.20.6–26.3 on Bukkit-compatible
servers. The same JAR retains Java 21 bytecode and the Spigot 1.20.6 API floor.

## Changes

- Extend the existing broad-version compatibility work to 26.3.
- Recognize natural poplar logs and red, orange, and yellow poplar leaves by runtime material name.
- Keep poplar wood and stripped variants excluded from tree shifting.
- Preserve the existing player-placed log, persistent leaf, collision, and chunk protections.
- Track the plugin version as a Gradle resource input so version changes update `plugin.yml` even on
  incremental builds.

## Verified

- `./gradlew clean build` with Temurin JDK 21: 53 tests, zero failures or errors.
- JAR metadata: plugin version `1.3.0`, API version `1.20.6`.
- Paper 1.20.6 build 151, Java 21: plugin enables; oak classification, natural-tree detection,
  physical log shifting, axis preservation, leaf support/eligibility, persistent leaf protection,
  player-placed log protection, processed wood rejection, status, reload, and shutdown pass.
- Paper 26.3 build 169 (Paper labels this build BETA), Java 25: the same checks pass for oak, pale oak,
  and each poplar leaf color. The final 1.3.0 JAR was used on both servers.
- Mojang's version manifest reports `26.3` as the latest stable release and `26.4-snapshot-3` as the
  latest snapshot. Paper's API confirms Java 25 is required for 26.3.

Runtime checks used a temporary companion plugin on disposable localhost-only servers. It created
blocks, called TimberShift's actual world adapters, detector, planner, executor, and leaf support
checker, and asserted the resulting block states. It did not simulate a connected Survival player.
The test servers shut down after completing the checks.

## Remaining manual checks

Follow [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for the remaining in-game checks, especially connected-player
chopping, vanilla item drops/durability, scheduled leaf decay and loot, natural generated poplar trees
and shelf mushrooms, protection-plugin interactions, and the intermediate server/Spigot/Purpur matrix.
Those cases were not verified in this run. No live server installation was performed.

## Artifact

`build/libs/TimberShift-1.3.0.jar`

SHA-256: `bfc23decd09ababc20918a3dc072f4a4e8b5fe427f858fbb8a97bdb2fe71bfe9`

## Version sources

- [Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
- [Minecraft Java 26.3 release notes](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3)
- [Paper 26.3 build metadata](https://fill.papermc.io/v3/projects/paper/versions/26.3/builds/169)
- [Paper 1.20.6 build metadata](https://fill.papermc.io/v3/projects/paper/versions/1.20.6/builds/151)
