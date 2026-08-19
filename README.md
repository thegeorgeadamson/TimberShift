<div align="center">

# TimberShift

**A better way to chop trees in Minecraft.**

Trees shift down as you chop them, one block at a time.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A)](#compatibility)
[![Paper](https://img.shields.io/badge/Paper-26.2-blue)](#compatibility)
[![Purpur](https://img.shields.io/badge/Purpur-26.2-9B59B6)](#compatibility)
[![Java](https://img.shields.io/badge/Java-25-orange)](#compatibility)
[![License](https://img.shields.io/badge/License-GPL--3.0-green)](LICENSE)

</div>

---

TimberShift makes chopping trees less annoying without turning it into an instant tree-felling mechanic.

Break the bottom of a tree and the remaining logs shift down by one block. You still have to chop **every log yourself** — you just don't have to pillar into the leaves to reach the last few.

```text id="v7htzq"
        🌿🌿🌿
      🌿🌿🌿🌿🌿
          🪵
          🪵
          🪵
      🧍  🪵  ← chop

            ↓

        🌿🌿🌿
      🌿🌿🌿🌿🌿

          🪵
          🪵
      🧍  🪵  ← keep chopping
```

<div align="center">

### One block broken. One block harvested.

</div>

## Why TimberShift?

I've always liked the convenience of tree-felling plugins, but most of them make chopping trees almost completely automatic. Break one log and the entire tree disappears.

TimberShift takes a different approach.

The actual chopping stays close to vanilla. Tool durability still matters, every log still needs to be broken, and you don't get an entire tree from a single swing.

**TimberShift just brings the rest of the tree down as you work through it.**

---

## Features

- **Progressive tree shifting** — trees move down one block at a time
- **Vanilla-style chopping** — every log still needs to be broken individually
- **Normal drops & durability** — only the block you actually chop is harvested
- **Smart tree detection** — avoids blindly treating every group of logs as a tree
- **Large tree support** — handles branching trees and 2×2 trunks
- **BlockData preservation** — moved logs keep their orientation and state
- **Sneak bypass** — hold sneak to chop normally
- **Fast leaf decay** — optional, local and configurable
- **Player leaf protection** — persistent/player-placed leaves aren't fast-decayed
- **No `randomTickSpeed` changes**
- **Per-world control** — whitelist or blacklist worlds
- **Per-player toggle**
- **Protection friendly** — respects cancelled block-break events
- **BentoBox / AOneBlock friendly**
- **No required dependencies**

---

## How it works

There isn't anything special players need to learn.

Walk up to a tree and chop it normally.

When TimberShift recognises a valid tree, the remaining structure shifts down by one block after you break a log.

```text id="gj47w3"
START              CHOP #1             CHOP #2

   🪵                  🪵
   🪵                  🪵
   🪵                  🪵
🧍 🪵 ← chop        🧍 🪵 ← chop        🧍 🪵 ← chop
```

Keep chopping from roughly the same position until you're done.

TimberShift **does not** harvest the shifted logs. They're real blocks that still need to be broken normally.

### Want normal behaviour?

Hold **sneak** while chopping.

TimberShift will leave the tree alone and Minecraft handles the block normally.

---

## Smart tree detection

TimberShift doesn't use the usual:

> *"It's a log, so it must be a tree."*

Before anything moves, the plugin checks the structure and surrounding foliage to determine whether what you're chopping actually looks like a tree.

It's designed to handle normal vanilla trees including:

| | | |
| --- | --- | --- |
| Oak | Spruce | Birch |
| Jungle | Acacia | Dark Oak |
| Mangrove | Cherry | Pale Oak |

Large trees, branches and 2×2 trunks are handled as well.

Detection and movement are deliberately conservative. If TimberShift can't safely move something, it would rather leave it alone than decide your log cabin is a tree.

---

## Fast leaf decay

Fast leaf decay can optionally clean up the canopy after a tree has actually been harvested.

```yaml id="t8zpmu"
leaves:
  fast-decay:
    enabled: true
```

This system runs independently of Minecraft's random tick system.

TimberShift **never changes**:

```text id="dujglv"
/gamerule randomTickSpeed
```

That means enabling fast leaf decay won't speed up crops, grass, fire, saplings or anything else that relies on random ticks.

### Player-placed leaves

Player-placed leaves are left alone.

TimberShift checks the persistent state of leaves before accelerating their decay. Persistent leaves are never intentionally removed by the fast-decay system.

Natural leaves are also checked again before they're removed. If a leaf is still supported by another nearby tree, TimberShift leaves it there.

---

## Installation

### Requirements

- Minecraft Java Edition **26.2**
- Java **25**
- Paper, Purpur or Spigot

### Install

1. Download the latest `TimberShift.jar` from **Releases**.
2. Stop your server.
3. Drop the JAR into your `plugins` directory.
4. Start the server.

That's it.

```text id="0xmv55"
server/
├── plugins/
│   └── TimberShift.jar
└── ...
```

On first start, TimberShift creates:

```text id="5i5u31"
plugins/
└── TimberShift/
    └── config.yml
```

No other plugins are required.

---

## Compatibility

| Platform | Version | Status |
| :--- | :---: | :---: |
| **Paper** | 26.2 | ✅ Supported |
| **Purpur** | 26.2 | ✅ Supported |
| **Spigot** | 26.2 | ✅ Supported |
| **Java** | 25 | ✅ Required |

> [!NOTE]
> Paper and Purpur are the primary development targets. TimberShift stays on the standard Bukkit/Spigot API where practical and avoids NMS and CraftBukkit internals.

---

## Configuration

TimberShift is designed to work well out of the box, but most of the behaviour you'd reasonably want to change is configurable.

<details>
<summary><strong>Example config.yml</strong></summary>

<br>

```yaml id="9cx0md"
config-version: 1

general:
  enabled: true
  debug: false

activation:
  require-axe: true
  require-permission: true
  sneak-bypasses: true

worlds:
  mode: BLACKLIST
  list: []

tree-detection:
  require-natural-leaves: true

  limits:
    max-logs: 256
    max-height: 48
    max-horizontal-radius: 12
    max-scanned-blocks: 1500

movement:
  blocks-per-chop: 1
  abort-on-unloaded-chunk: true
  preserve-block-data: true

leaves:
  fast-decay:
    enabled: true
    initial-delay-ticks: 10
    interval-ticks: 2
    max-leaves-per-batch: 32
    max-leaves-per-tree: 512
    max-radius: 12
```

</details>

The generated configuration includes comments explaining each option.

After making changes, reload TimberShift with:

```text id="rmy54r"
/timbershift reload
```

There's no need to restart the server.

> [!IMPORTANT]
> Bukkit's `/reload` command isn't recommended. Use TimberShift's own reload command instead.

---

## World control

TimberShift can be enabled globally or restricted to specific worlds.

### Blacklist

Enable TimberShift everywhere except the listed worlds:

```yaml id="6zckpb"
worlds:
  mode: BLACKLIST
  list:
    - lobby
    - creative
```

### Whitelist

Only enable TimberShift in the listed worlds:

```yaml id="tjdgzu"
worlds:
  mode: WHITELIST
  list:
    - world
    - oneblock_world
```

This is particularly useful on servers with multiple game modes.

---

## Commands

| Command | Description |
| --- | --- |
| `/timbershift` | Show TimberShift information |
| `/timbershift help` | Show available commands |
| `/timbershift toggle` | Toggle TimberShift for yourself |
| `/timbershift status` | Show your current TimberShift status |
| `/timbershift reload` | Reload the configuration |

All commands are also available through the shorter `/ts` alias.

---

## Permissions

| Permission | Default | Description |
| --- | :---: | --- |
| `timbershift.use` | Everyone | Use TimberShift |
| `timbershift.toggle` | Everyone | Toggle TimberShift |
| `timbershift.command.status` | Everyone | View TimberShift status |
| `timbershift.command.reload` | OP | Reload the configuration |
| `timbershift.admin` | OP | Administrative permissions |

---

## BentoBox & AOneBlock

TimberShift works alongside BentoBox and AOneBlock without depending on either plugin.

If BentoBox or another protection plugin cancels a block break, TimberShift doesn't use that attempted break to move the tree.

That means island protection continues to decide what a player is actually allowed to break.

For a dedicated OneBlock server, you can also restrict TimberShift to your AOneBlock worlds:

```yaml id="s3yn8v"
worlds:
  mode: WHITELIST
  list:
    - oneblock_world
```

---

## Performance & safety

Minecraft trees can get surprisingly weird, so TimberShift puts hard limits around tree detection and movement.

It doesn't perform unlimited searches through connected blocks and won't force-load chunks just to finish scanning a tree.

Before anything is moved, TimberShift:

1. Detects the tree within configured limits.
2. Calculates where each block needs to move.
3. Checks that those destinations are safe.
4. Captures the original block data.
5. Applies the movement only after validation.

This prevents a naive block-by-block movement from overwriting logs or unrelated terrain.

Fast leaf decay is also processed in small batches rather than deleting a large canopy in one server tick.

---

## Debugging

If TimberShift isn't recognising a particular tree, enable debug logging:

```yaml id="j32x0a"
general:
  debug: true
```

Debug output includes useful information about things like:

- tree detection
- rejected structures
- scan limits
- blocked destinations
- unloaded chunks
- leaf detection

Turn it back off once you're finished troubleshooting.

---

## Building from source

You'll need **JDK 25** and Git.

```bash id="09b4ks"
git clone https://github.com/thegeorgeadamson/TimberShift.git
cd TimberShift

./gradlew clean build
```

The compiled plugin will be available in:

```text id="bkjl4m"
build/libs/
```

---

## Bugs & contributions

Found a tree that TimberShift doesn't like?

Open an issue and, if possible, include:

- your server software and version
- TimberShift version
- tree type
- relevant config changes
- a screenshot of the tree
- steps to reproduce it

Trees growing into each other, large oaks and jungle trees are particularly useful test cases.

Pull requests are welcome.

When contributing, keep the main idea of TimberShift in mind:

> **Make chopping trees more convenient without removing the chopping.**

World safety and server performance take priority over trying to handle every bizarre arrangement of logs perfectly.

---

## License

TimberShift is open source under the **GNU General Public License v3.0**.

See [`LICENSE`](LICENSE) for the full license.

---

<div align="center">

Made by **George Adamson**

**Chop the tree. Let the tree come down to you.**

</div>
