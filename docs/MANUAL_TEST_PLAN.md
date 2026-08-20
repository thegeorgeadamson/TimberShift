# TimberShift 26.2 manual integration test plan

Run this checklist on a disposable Paper or Purpur 26.2 staging server with Java 25. Repeat the
protection cases with the production server's actual protection configuration before deployment.

## Preparation

1. Run `./gradlew clean build` and copy `build/libs/TimberShift-1.2.0.jar` to `plugins/`.
2. Start the server, confirm TimberShift enables without warnings, and run `/timbershift status`.
3. Keep `blocks-per-chop: 1`, enable debug logging for rejection diagnostics, and use a Survival axe
   unless a case says otherwise.
4. For each tree, count logs before and collected log items after fully chopping it. Confirm one item per
   legitimate Survival break (subject to normal enchantments), normal axe durability, no duplicated
   drops, and preserved visible log axes.

## Tree forms

- [ ] 1. Oak: break the lowest log repeatedly; the remaining safe trunk shifts one block per break.
- [ ] 2. Large oak: verify branches move only into air and blocked branches remain intact.
- [ ] 3. Birch: verify a straight trunk completes from ground level.
- [ ] 4. Spruce: verify the crown and leaf decay remain vanilla-like.
- [ ] 5. Large/2x2 spruce: verify only freed columns shift and no neighboring base log is overwritten.
- [ ] 6. Jungle: verify vines/cocoa do not cause block replacement or duplicate drops.
- [ ] 7. 2x2 jungle: verify staggered column movement remains deterministic and item count is exact.
- [ ] 8. Acacia: verify diagonal branches either safely descend or remain where blocked.
- [ ] 9. Dark oak: verify 2x2 behavior and no terrain replacement.
- [ ] 10. Mangrove: verify roots and water are treated as collisions, never overwritten.
- [ ] 11. Cherry: verify matching, non-persistent cherry leaves validate the tree.
- [ ] 12. Pale oak: verify pale oak logs/leaves classify and shift correctly.

## False positives and collisions

- [ ] 13. Player-built log pillar with no natural leaves: no shift.
- [ ] 14. Log house/wall, including persistent decorative leaves: no shift and no block changes.
- [ ] 15. Natural tree touching a building above its base: conservative rejection or safe non-overlapping
  movement only; no building block may change.
- [ ] Add water, a solid block, leaves, and terrain beneath separate branches: each blocked branch stays
  intact and no destination is overwritten.
- [ ] Rotate several source logs to X and Z axes: axes remain identical after movement.

## Chunks and protection

- [ ] 16. Tree at a loaded chunk boundary: behavior is normal and bounded.
- [ ] 17. Unload the adjacent chunk while the tree touches its boundary: with the default setting the
  shift aborts and the plugin does not load the chunk.
- [ ] 18. Tree in an area where the player is allowed to build: an allowed break shifts normally.
- [ ] 19. Attempt the same break in an area protected from that player: the break is cancelled and
  TimberShift changes no log, including on the following tick.
- [ ] Repeat case 19 for spawn protection and every installed claim/WorldGuard-style plugin.
- [ ] Place a tree across an island/claim boundary and verify the server's intended policy; restrict
  TimberShift world access if the protection stack cannot express cross-boundary movement safely.

## Activation and game modes

- [ ] 20. Sneak bypass: the selected log breaks normally and nothing shifts.
- [ ] 21. Axe versus non-axe: an axe shifts; a bare hand or other tool does not by default.
- [ ] 22. Creative mode: original breaks create no Survival drops; moved logs create no drops.
- [ ] 23. Survival mode: drops, enchantments, statistics, and durability remain vanilla for each break.
- [ ] Remove `timbershift.use`: chopping is fully vanilla.
- [ ] Test blacklist and whitelist world modes, including case differences in configured world names.

## Concurrency and lifecycle

- [ ] 24. Two players chop the same tree on nearby ticks: no duplicate blocks/items and no deletion.
- [ ] 25. Rapidly break successive reachable logs: every successful break produces at most one shift.
- [ ] Modify or piston-move a planned source/destination immediately after a break: revalidation aborts
  instead of overwriting the change.
- [ ] Explode part of a recently shifted tree: stale trusted positions do not expand into other logs.
- [ ] 26. Edit valid and invalid settings, then run `/timbershift reload`: valid values activate without
  duplicate listeners/tasks; invalid values warn and fall back or retain the old config on YAML failure.
- [ ] 27. Restart the server: plugin enables once, no task/state leak appears, and normal detection resumes.
- [ ] 28. Toggle off, restart, and run `/timbershift status`: the player preference remains off; toggle on
  and confirm normal behavior returns.

## Commands and shutdown

- [ ] Start the server and verify the TimberShift wordmark shows the author, plugin version, Minecraft
  version, fast-decay state, and a plausible measured load time without obscuring other startup logs.
- [ ] Run help/status/reload from console and as permitted/unpermitted players; output is concise and no
  command throws.
- [ ] Verify `/ts` alias and tab completion.
- [ ] Stop the server while shifts may be pending; shutdown completes cleanly with no repeated task or
  exception.

## Fast leaf decay

- [ ] Grow and fully chop an oak: unsupported natural leaves decay progressively after the configured
  delay in small, shuffled groups rather than in one synchronous burst.
- [ ] Set `leaves-per-step` to 1 and then 4, reloading between tests: the visible per-tree pace changes
  while the global batch cap remains respected.
- [ ] Place leaves manually, remove every nearby log, and wait: persistent leaves remain indefinitely.
- [ ] Put persistent leaves directly against a natural canopy: only eligible natural leaves disappear.
- [ ] Grow two same-type trees with touching canopies, chop one, and verify the other tree's supported
  canopy remains.
- [ ] After the previous supported pass finishes, manually break the natural leaf bridge between the
  canopies: remembered unsupported leaves resume fast decay while the logged tree remains protected.
- [ ] Chop only the first bottom log of a tall TimberShift tree, then pause longer than the configured
  initial delay: leaves supported by shifted upper logs do not disappear prematurely.
- [ ] Finish the same tree: its now-unsupported natural canopy decays quickly.
- [ ] Over repeated oaks, verify normal apple rolls; verify sapling and stick drops without shears or
  Fortune behavior. Then disable `preserve-vanilla-drops` and verify accelerated leaves drop nothing.
- [ ] Disable `leaves.fast-decay.enabled`, reload, and verify only vanilla leaf behavior remains.
- [ ] Test large jungle and dark-oak canopies while watching tick health and configured batch limits.
- [ ] Test a canopy at a loaded chunk boundary, then repeat with the neighboring chunk unloaded; no
  chunk is force-loaded and questionable leaves are skipped.
- [ ] Have several players finish trees concurrently; global batches remain bounded and drops do not
  duplicate.
- [ ] Repeat next to a protected-region boundary. A cancelled chop queues nothing; confirm the server's
  intended policy for foliage that crosses a protection boundary.
- [ ] Set `randomTickSpeed` to a non-default value, then to zero: TimberShift never changes it, and its
  configured accelerated decay continues independently after unsupported leaves are queued.
- [ ] Reload while decay is pending, both enabled and disabled: old pending work clears and no duplicate
  scheduler appears.
- [ ] Restart while decay is pending: startup is clean and old pending work does not return.

## Player-placed log provenance

- [ ] Place a log beside or inside a natural tree after TimberShift is installed, then chop around it:
  the placed log is never shifted and may conservatively stop the operation.
- [ ] Restart the server and repeat around that placed log: the protection record survives chunk save
  and reload.
- [ ] Move a recorded log with an extending and retracting piston: its destination remains protected.
- [ ] Break, burn, and explode recorded logs in separate tests, then place/grow a natural tree at those
  coordinates: stale placement records do not incorrectly block the later tree.
- [ ] Confirm a pre-existing log building with persistent decorative leaves is still rejected by the
  normal leaf/structure checks; placement provenance cannot be reconstructed retroactively.

Record the server implementation/build, protection-plugin versions, TimberShift config, results, and
any debug rejection lines with the deployment notes.
