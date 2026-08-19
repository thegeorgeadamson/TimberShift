TimberShift

TimberShift is a small Minecraft plugin that makes chopping trees less annoying without turning it into an instant tree-felling mechanic.

When you break the bottom of a tree, the remaining logs shift down by one block. You still have to chop every log yourself — you just don’t have to pillar up into the leaves to get the last few.

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

That’s basically it. One block broken, one block harvested.

Why?

I’ve always liked the convenience of tree-felling plugins, but most of them make chopping trees almost completely automatic. Break one log and the entire tree explodes into items.

TimberShift takes a different approach.

The actual chopping stays close to vanilla. Tool durability still matters, every log still has to be broken, and you don’t get an entire tree from a single swing. The plugin just brings the rest of the tree down as you work through it.

Features

* Trees shift down one block at a time as they’re chopped
* Every log still has to be broken individually
* Works with normal vanilla tree types, including larger and branching trees
* Preserves log orientation and block data when logs move
* Tries to distinguish real trees from player-built structures
* Sneak while chopping to bypass TimberShift
* Optional fast leaf decay
* Player-placed leaves aren’t affected by fast decay
* Doesn’t touch randomTickSpeed
* Per-world whitelist/blacklist
* Player toggle
* Configurable detection and performance limits
* Designed to play nicely with protection plugins
* No required dependencies

Compatibility

TimberShift is built for Minecraft 26.2.

Server	Support
Paper 26.2	✅
Purpur 26.2	✅
Spigot 26.2	✅

Java 25 is required.

Paper and Purpur are the main targets. TimberShift sticks to the standard Bukkit/Spigot API where possible and doesn’t use NMS or CraftBukkit internals.

Installation

Download the latest TimberShift JAR and put it in your server’s plugins folder:

plugins/
└── TimberShift.jar

Restart the server and TimberShift will create its configuration in:

plugins/TimberShift/config.yml

There are no required dependencies.

Using TimberShift

There’s nothing special you need to do.

Grab an axe and chop a tree normally.

When TimberShift recognises the structure as a tree, breaking a log near the bottom will shift the remaining tree down one block.

Keep chopping from the same position until you’re done.

If you want to break a log normally without TimberShift doing anything, hold sneak while breaking it.

Fast leaf decay

TimberShift can also clean up leaves faster once a tree has actually been chopped down.

This is optional:

leaves:
  fast-decay:
    enabled: true

Fast decay doesn’t work by changing the world’s random tick speed. TimberShift handles it separately, so crops and everything else affected by randomTickSpeed are left alone.

It also checks the leaf’s persistent state. Leaves placed by players are persistent and won’t be removed by TimberShift.

Natural leaves are checked again before they’re decayed, so a canopy that’s still supported by another nearby tree should be left alone.

Configuration

The default config is intended to work without much tweaking, but the main behaviour can be changed.

A shortened example:

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

The generated config.yml contains comments explaining the available options.

Limiting TimberShift to certain worlds

By default, the world list works as a blacklist:

worlds:
  mode: BLACKLIST
  list:
    - lobby
    - creative

Or you can switch it to a whitelist:

worlds:
  mode: WHITELIST
  list:
    - world
    - oneblock_world

Commands

Command	Description
/timbershift	Shows TimberShift information
/timbershift help	Shows available commands
/timbershift toggle	Toggles TimberShift for yourself
/timbershift status	Shows the current status
/timbershift reload	Reloads the configuration

/ts can be used as a shorter alias.

Permissions

Permission	Default	Description
timbershift.use	Everyone	Use TimberShift
timbershift.toggle	Everyone	Toggle TimberShift
timbershift.command.status	Everyone	View TimberShift status
timbershift.command.reload	OP	Reload the configuration
timbershift.admin	OP	Administrative permissions

Tree detection

The plugin deliberately doesn’t shift every connected log it can find.

Before moving anything, TimberShift looks at the log structure and surrounding natural leaves to decide whether what you’re chopping actually resembles a tree. Detection is bounded by configurable size and distance limits.

Before a shift happens, the movement is planned and checked first. TimberShift won’t intentionally overwrite an unrelated solid block just to make a tree move.

This means there may be unusual or heavily modified trees that TimberShift decides not to move. I’d rather have it leave a weird tree alone than decide your log cabin needs to move down a block.

And if you’re working with logs in a build, sneaking gives you an easy way to bypass detection entirely.

BentoBox / OneBlock

TimberShift works alongside BentoBox and AOneBlock without requiring either of them as a dependency.

It respects cancelled block-break events, so if BentoBox or another protection plugin says you aren’t allowed to break a log, TimberShift won’t use that attempted break to shift the tree.

For a OneBlock server, you can also whitelist only the worlds where you want TimberShift enabled.

Performance

Tree detection has hard limits rather than doing an unlimited search through connected blocks.

TimberShift also won’t force-load chunks to finish scanning a tree.

Fast leaf decay is processed in small batches instead of trying to remove a large canopy in a single tick.

Most of this should be invisible while playing, but it’s there to stop one enormous or deliberately weird structure from turning a log break into an expensive server-wide operation.

Building from source

You’ll need JDK 25.

git clone https://github.com/thegeorgeadamson/TimberShift.git
cd TimberShift
./gradlew clean build

The finished JAR will be in:

build/libs/

Found a bug?

If TimberShift does something strange with a particular tree, open an issue and include the tree type, your server software/version and, if possible, a screenshot of the tree before it was chopped.

Trees in Minecraft can get surprisingly weird, particularly large oaks, jungle trees and trees growing into each other, so reproducible examples are useful.

Pull requests are welcome too.

License

TimberShift is licensed under the GNU General Public License v3.0.

Credits

Created and maintained by George Adamson.

The idea is simple: keep Minecraft tree chopping feeling like tree chopping, just without having to climb up after the last log.
