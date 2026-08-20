<div align="center">

# TimberShift

**Progressive tree chopping for Minecraft servers.**

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62b47a?style=flat-square)](#requirements)
[![Java 25](https://img.shields.io/badge/Java-25-e76f00?style=flat-square)](#requirements)
[![Latest release](https://img.shields.io/github/v/release/thegeorgeadamson/TimberShift?style=flat-square&label=release)](https://github.com/thegeorgeadamson/TimberShift/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square)](LICENSE)

[Download](https://github.com/thegeorgeadamson/TimberShift/releases/latest) · [Discord support](https://discord.gg/VmQAzmYyvA) · [Report an issue](https://github.com/thegeorgeadamson/TimberShift/issues)

</div>

TimberShift is a Minecraft server plugin that brings the remaining logs of a tree down as you chop it.
It is deliberately not a tree-felling plugin: every log still has to be broken by a player.

Break a reachable log near the base of a natural tree and the logs that can move safely shift down by
one block. The original break, item drop, tool damage, enchantments, and statistics are left to
Minecraft.

## Requirements

- Minecraft Java Edition 26.2
- Java 25
- Paper 26.2, Purpur 26.2, or Spigot 26.2

Paper and Purpur are the primary targets. TimberShift is compiled against the common Spigot API and
does not use NMS, CraftBukkit internals, or a server-specific plugin loader.

The current build is deliberately targeted at Minecraft 26.2. If you need another Minecraft version,
[ask in the support server](https://discord.gg/VmQAzmYyvA); support for other versions can be considered
on request where the server APIs allow it.

## Installation

1. Download `TimberShift-1.2.0.jar` from the [latest release](../../releases/latest).
2. Stop the server and place the JAR in its `plugins` directory.
3. Start the server. TimberShift will create `plugins/TimberShift/config.yml`.

No other plugins are required.

## What it does

- Moves eligible tree logs down progressively instead of harvesting a whole tree at once.
- Preserves each moved log's block data, including its axis.
- Handles straight trunks, branches, and 2x2 trees conservatively.
- Uses bounded natural-tree detection to avoid treating every wooden structure as a tree.
- Never replaces terrain or an unrelated block to complete a move.
- Leaves the player's original block break to normal Minecraft mechanics.
- Supports a sneak-to-bypass option, per-player toggles, and per-world allow/deny lists.
- Observes cancelled block-break events and does nothing when the original break is denied.
- Records player-placed logs in persistent chunk data and excludes them from tree movement.
- Includes optional, locally scheduled fast leaf decay.

If part of a tree cannot move safely, TimberShift leaves that part where it is. This is intentional: an
unusual tree occasionally staying put is preferable to damaging a build.

## Fast leaf decay

Fast leaf decay is enabled by default. It only considers bounded candidates associated with a tree that
TimberShift has already recognised.

Before removing a queued leaf, TimberShift checks the live block again. The leaf must still be natural,
non-persistent, loaded, and unsupported by a nearby log. Persistent/player-placed leaves are never
intentionally removed. A cancellable Bukkit `LeavesDecayEvent` is also fired before removal.

With `preserve-vanilla-drops` enabled, leaf removal uses the server's normal no-tool block loot path for
applicable saplings, sticks, apples, and other drops. TimberShift does not apply shears or Fortune and
does not maintain its own copy of Minecraft's loot tables.

Leaves are shuffled and removed in small visible steps. The per-tree `leaves-per-step` setting controls
the pace, while `max-leaves-per-batch` remains a global safety ceiling when several trees are active.
If a supported canopy was skipped because it touched another tree, breaking a connecting natural leaf
or the start of vanilla leaf decay wakes that remembered canopy for another safe pass.

The system uses one bounded central queue. It never changes or depends on `randomTickSpeed`, and it does
not force-load chunks. Only canopies already associated with a verified TimberShift tree are woken by
later leaf changes.

```yaml
leaves:
  fast-decay:
    enabled: true
    initial-delay-ticks: 10
    interval-ticks: 2
    leaves-per-step: 2
    max-leaves-per-batch: 32
    max-leaves-per-tree: 512
    max-radius: 12
    max-scanned-blocks: 2048
    max-active-operations: 32
    preserve-vanilla-drops: true
    effects:
      particles: true
      sounds: false
```

## Configuration

The generated [`config.yml`](src/main/resources/config.yml) documents every option. The main sections
control activation, world filtering, tree-detection limits, movement, leaf decay, effects, and messages.

Minecraft exposes an exact persistence flag for player-placed leaves, which TimberShift always checks.
Ordinary logs have no equivalent vanilla flag. With `protect-player-placed-logs: true`, TimberShift
therefore records log placements it observes while installed in the owning chunk's persistent plugin
data. Those records survive restarts and follow piston movement. Logs placed before TimberShift was
installed cannot be identified retrospectively, so the natural-leaf and structure checks remain in
place as the fallback protection.

Reload changes with:

```text
/timbershift reload
```

Use TimberShift's command rather than Bukkit's `/reload`. Pending accelerated leaf work is safely
cleared when the configuration is reloaded.

## Commands

| Command | Description |
| --- | --- |
| `/timbershift help` | Show command help |
| `/timbershift toggle` | Toggle TimberShift for yourself |
| `/timbershift status` | Show the current global, leaf-decay, and player state |
| `/timbershift reload` | Reload `config.yml` |

`/ts` is available as a shorter alias.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `timbershift.use` | Everyone | Use tree shifting |
| `timbershift.toggle` | Everyone | Change your own toggle |
| `timbershift.command.status` | Everyone | View status |
| `timbershift.command.reload` | Operators | Reload configuration |
| `timbershift.admin` | Operators | Grant all TimberShift permissions |

## Building

The repository includes a pinned Gradle wrapper. With JDK 25 available:

```bash
./gradlew clean build
```

The plugin JAR is written to `build/libs/TimberShift-1.2.0.jar`.

## Reporting a problem

For help or a version-support request, join the [TimberShift support server](https://discord.gg/VmQAzmYyvA).
For reproducible bugs, [open a GitHub issue](https://github.com/thegeorgeadamson/TimberShift/issues)
with the TimberShift version, server software and build, relevant configuration, tree type,
reproduction steps, and a screenshot when the tree shape matters.

## License

TimberShift is available under the [GNU General Public License v3.0](LICENSE).
