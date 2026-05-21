package wile.redstonepen.blocks.track

import java.util.Locale
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections
import wile.redstonepen.util.Auxiliaries

/**
 * Computes the redstone network topology for a single [RedstoneTrackBlock] position.
 *
 * Takes a snapshot of the world state at construction time and produces a [Result] with no side
 * effects. The caller ([TrackBlockEntity.updateConnections]) is responsible for applying the
 * result: writing the new nets and state flags back to the block entity, recursing into
 * [Result.trackConnectionUpdates], and sending [Result.neighboursToNotify] to the level.
 *
 * The pipeline runs in five stages:
 * 1. Snapshot previous net powers into a per-side array so power is preserved across topology
 *    changes.
 * 2. Partition wire/connector bits into internal routes (two sides share an edge wire) versus
 *    external routes (each side connects to the outside world independently).
 * 3. Walk external routes, query adjacent blocks, and assemble [TrackNet] objects.
 * 4. Zero the power bits for any side that is no longer part of a net.
 * 5. Collect adjacent [TrackBlockEntity] instances that were connected before but are not now, so
 *    they can recalculate their own topology.
 */
internal class TrackNetworkCalculator(
    private val level: Level,
    private val pos: BlockPos,
    private val stateFlags: TrackStateFlags,
    private val block: RedstoneTrackBlock,
    private val trace: Boolean,
) {
    /**
     * @property nets The newly computed nets for this position.
     * @property newStateFlags [stateFlags] with power bits cleared for any side not in a net.
     * @property trackConnectionUpdates Adjacent track block entities that must recalculate.
     *   Includes both newly connected neighbours (so they adopt the updated topology) and
     *   previously connected neighbours that are no longer reachable (so they drop stale power).
     * @property neighboursToNotify All block positions that should receive a neighbour-changed
     *   event, covering every position that was or is in a powered net.
     */
    data class Result(
        val nets: List<TrackNet>,
        val newStateFlags: TrackStateFlags,
        val trackConnectionUpdates: Set<TrackBlockEntity>,
        val neighboursToNotify: Set<BlockPos>,
    )

    /**
     * Runs the full topology pipeline and returns the new network state.
     *
     * @param previousNets The nets from the last calculation. Required to seed per-side power
     *   values before net objects are discarded and rebuilt; a wire that remains connected would
     *   otherwise silently lose its stored power during the rebuild.
     * @return The new nets, updated state flags, adjacent track entities to recalculate, and block
     *   positions to notify with a neighbour-changed event.
     */
    fun calculate(previousNets: List<TrackNet>): Result {
        val allNeighbours = HashSet<BlockPos>()
        val currentSidePowers = IntArray(6)
        val internalConnectedSides = LongArray(6)
        val externalConnectedRoutes = LongArray(6)
        val trackConnectionUpdates = mutableSetOf<TrackBlockEntity>()

        snapshotPreviousState(previousNets, allNeighbours, currentSidePowers)
        computeConnectionRouting(internalConnectedSides, externalConnectedRoutes)

        val (newNets, usedSides) =
            buildNets(
                internalConnectedSides,
                externalConnectedRoutes,
                currentSidePowers,
                trackConnectionUpdates,
            )

        val newStateFlags = zeroUnusedSidePowers(usedSides)

        collectDisconnectedTrackUpdates(newNets, allNeighbours, trackConnectionUpdates)

        newNets
            .filter { it.power > 0 }
            .forEach { net -> allNeighbours.addAll(net.neighbour_positions) }

        return Result(newNets, newStateFlags, trackConnectionUpdates, allNeighbours)
    }

    /**
     * Seeds per-side power values and the previous neighbour set before the new topology is built.
     * Power must be read now because net objects are discarded and rebuilt from scratch; any net
     * that survives the recalculation inherits its power from this snapshot.
     *
     * @param previousNets Nets from the last calculation.
     * @param allNeighbours Accumulator; all neighbour positions from previous nets are added here.
     * @param currentSidePowers Accumulator; indexed by [Direction.ordinal], filled with the power
     *   level that each internal side carried in the previous nets.
     */
    private fun snapshotPreviousState(
        previousNets: List<TrackNet>,
        allNeighbours: HashSet<BlockPos>,
        currentSidePowers: IntArray,
    ) {
        previousNets.forEach { net ->
            net.internal_sides.forEach { ps -> currentSidePowers[ps.ordinal] = net.power }
            allNeighbours.addAll(net.neighbour_positions)
        }
        if (trace) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "UCON: %s SIDPW: [%01x %01x %01x %01x %01x %01x]",
                    TrackBlockEntity.posstr(pos),
                    currentSidePowers[0],
                    currentSidePowers[1],
                    currentSidePowers[2],
                    currentSidePowers[3],
                    currentSidePowers[4],
                    currentSidePowers[5],
                )
            )
        }
    }

    /**
     * Partitions the wire and connector bits in [stateFlags] into internal and external groups.
     *
     * A wire bit pair that appears on two sides of a shared edge is *internal*: both sides are
     * joined inside the block and do not reach the outside world independently. All remaining bits
     * are *external* and connect to adjacent blocks.
     *
     * After collecting internal groups, a two-pass union-find merges any groups that share a wire
     * bit (a side can belong to at most one group after merging). External connectivity bits are
     * then assigned to the group that owns each direction, or to the direction's own slot if it has
     * no internal group.
     *
     * @param internalConnectedSides Output: maps each group's representative index to the combined
     *   wire bits of all sides in that group.
     * @param externalConnectedRoutes Output: maps each index to the external wire and connector
     *   bits reachable from that group.
     */
    private fun computeConnectionRouting(
        internalConnectedSides: LongArray,
        externalConnectedRoutes: LongArray,
    ) {
        var externalConnectionFlags =
            stateFlags.raw and
                (RedstoneTrackDefs.STATE_FLAG_WIR_MASK or RedstoneTrackDefs.STATE_FLAG_CON_MASK)
        for ((wireBitPair, _) in connections.INTERNAL_EDGE_CONNECTION_MAPPING) {
            if ((stateFlags.raw and wireBitPair) != wireBitPair) continue
            externalConnectionFlags = externalConnectionFlags and wireBitPair.inv()
            for (i in 0 until 6) {
                if (((0xfL shl (4 * i)) and wireBitPair) == 0L) continue
                internalConnectedSides[i] = internalConnectedSides[i] or wireBitPair
            }
        }
        if (trace) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "UCON: %s CONFL: ext:%08x | int:[%08x %08x %08x %08x %08x %08x]",
                    TrackBlockEntity.posstr(pos),
                    externalConnectionFlags,
                    internalConnectedSides[0],
                    internalConnectedSides[1],
                    internalConnectedSides[2],
                    internalConnectedSides[3],
                    internalConnectedSides[4],
                    internalConnectedSides[5],
                )
            )
        }
        for (k in 0 until 2) {
            for (i in 0 until 6) {
                if (internalConnectedSides[i] == 0L) continue
                for (j in i + 1 until 6) {
                    if ((internalConnectedSides[i] and internalConnectedSides[j]) == 0L) continue
                    internalConnectedSides[i] =
                        internalConnectedSides[i] or internalConnectedSides[j]
                    internalConnectedSides[j] = 0L
                }
            }
        }
        for (i in 0 until 6) {
            if (internalConnectedSides[i] != 0L) {
                for (j in i until 6) {
                    val mask = 0xfL shl (4 * j)
                    if ((internalConnectedSides[i] and mask) == 0L) continue
                    val bulk = 0x1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + j)
                    externalConnectedRoutes[i] =
                        externalConnectedRoutes[i] or (externalConnectionFlags and (mask or bulk))
                    externalConnectionFlags = externalConnectionFlags and (mask or bulk).inv()
                }
            } else {
                val mask = 0xfL shl (4 * i)
                val bulk = 0x1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + i)
                externalConnectedRoutes[i] =
                    externalConnectedRoutes[i] or (externalConnectionFlags and (mask or bulk))
                externalConnectionFlags = externalConnectionFlags and (mask or bulk).inv()
            }
        }
        if (trace) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "UCON: %s CONSD: ext:%08x | int:[%08x %08x %08x %08x %08x %08x]",
                    TrackBlockEntity.posstr(pos),
                    externalConnectionFlags,
                    internalConnectedSides[0],
                    internalConnectedSides[1],
                    internalConnectedSides[2],
                    internalConnectedSides[3],
                    internalConnectedSides[4],
                    internalConnectedSides[5],
                )
            )
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "UCON: %s CONRT: ext:%08x | ext:[%08x %08x %08x %08x %08x %08x]",
                    TrackBlockEntity.posstr(pos),
                    externalConnectionFlags,
                    externalConnectedRoutes[0],
                    externalConnectedRoutes[1],
                    externalConnectedRoutes[2],
                    externalConnectedRoutes[3],
                    externalConnectedRoutes[4],
                    externalConnectedRoutes[5],
                )
            )
        }
    }

    /**
     * Translates the abstract routing computed by [computeConnectionRouting] into [TrackNet]
     * objects by querying the world.
     *
     * Each non-empty slot in [externalConnectedRoutes] becomes one net. Wire bits are resolved to
     * adjacent block positions via [discoverWireNeighbours]; bulk connector bits are resolved by
     * stepping directly to the neighbour in that direction. Each net's initial power is the maximum
     * power stored on any of its internal sides from the previous-state snapshot.
     *
     * @param internalConnectedSides Output of [computeConnectionRouting]: grouped internal wire
     *   bits.
     * @param externalConnectedRoutes Output of [computeConnectionRouting]: external bits per group.
     * @param currentSidePowers Per-side power snapshot produced by [snapshotPreviousState].
     * @param trackConnectionUpdates Accumulator; adjacent track entities discovered here are added.
     * @return The new nets paired with the set of directions that belong to at least one net. The
     *   direction set is passed to [zeroUnusedSidePowers] to clear stale power bits.
     */
    private fun buildNets(
        internalConnectedSides: LongArray,
        externalConnectedRoutes: LongArray,
        currentSidePowers: IntArray,
        trackConnectionUpdates: MutableSet<TrackBlockEntity>,
    ): Pair<List<TrackNet>, Set<Direction>> {
        val newNets = mutableListOf<TrackNet>()
        val usedSides = mutableSetOf<Direction>()
        for (i in 0 until 6) {
            if (externalConnectedRoutes[i] == 0L) continue
            val intSides = mutableSetOf<Direction>()
            val pwrSides = ArrayList<Direction>(6)
            val positions = ArrayList<BlockPos>(6)
            val extSides = ArrayList<Direction>(6)
            for (j in 0 until 6) {
                val mask = 0xfL shl (4 * j)
                val bulk = 0x1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + j)
                val side = connections.CONNECTION_BIT_ORDER[j]
                if ((internalConnectedSides[i] and mask) != 0L) intSides.add(side)
                if ((externalConnectedRoutes[i] and mask) != 0L) {
                    discoverWireNeighbours(
                        j,
                        side,
                        externalConnectedRoutes[i],
                        intSides,
                        pwrSides,
                        positions,
                        extSides,
                        trackConnectionUpdates,
                    )
                }
                if ((externalConnectedRoutes[i] and bulk) != 0L) {
                    val bulkPos = pos.relative(side)
                    val bulkState = level.getBlockState(bulkPos)
                    if (isRedstoneInsulator(bulkState)) continue
                    positions.add(bulkPos)
                    extSides.add(side.opposite)
                    intSides.add(side)
                    pwrSides.add(side)
                }
            }
            if (positions.isNotEmpty()) {
                val net = TrackNet(positions, extSides, ArrayList(intSides), ArrayList(pwrSides))
                net.power = net.internal_sides.maxOfOrNull { currentSidePowers[it.ordinal] } ?: 0
                newNets.add(net)
                usedSides.addAll(intSides)
            }
        }
        return Pair(newNets, usedSides)
    }

    /**
     * Resolves all wire bits in [routeFlags] for direction slot [j] to concrete neighbour positions
     * and adds them to the in-progress net being built by [buildNets].
     *
     * For each wire bit the lookup priority is:
     * 1. A matching track on the same face (straight connection) — adds the track and queues it for
     *    a topology update.
     * 2. Vanilla redstone wire directly below (`DOWN` side only) — adds the wire position.
     * 3. Any signal source — adds the source position.
     * 4. A matching track on the adjacent face (diagonal/corner connection) — the wire crosses an
     *    edge, so we step to the wire position and then one step in the face direction.
     * 5. Any non-insulating block — adds the position as a generic neighbour.
     *
     * The `diagonalCheck` flag is set when the straight target exists but the reciprocal wire bit
     * is absent, signalling the connection is not mutual and the diagonal path should be tried.
     *
     * @param j Direction slot index (0–5) into [connections.CONNECTION_BIT_ORDER].
     * @param side The [Direction] corresponding to slot [j].
     * @param routeFlags The external route flags for the current net group.
     * @param intSides Accumulator for internal sides belonging to this net.
     * @param pwrSides Accumulator for power-carrying sides belonging to this net.
     * @param positions Accumulator for neighbour block positions belonging to this net.
     * @param extSides Accumulator for the external-facing side at each neighbour position.
     * @param trackConnectionUpdates Accumulator; any adjacent track entities found are added here.
     */
    private fun discoverWireNeighbours(
        j: Int,
        side: Direction,
        routeFlags: Long,
        intSides: MutableSet<Direction>,
        pwrSides: MutableList<Direction>,
        positions: MutableList<BlockPos>,
        extSides: MutableList<Direction>,
        trackConnectionUpdates: MutableSet<TrackBlockEntity>,
    ) {
        for (k in 0 until 4) {
            val wireBit = 0x1L shl (4 * j + k)
            if ((routeFlags and wireBit) == 0L) continue
            val tsd = connections.getWireBitSideAndDirection(wireBit)
            val tsid = tsd.getA()
            val tdir = tsd.getB()
            val wirePos = pos.relative(tdir)
            val wireState = level.getBlockState(wirePos)
            var diagonalCheck = false
            if (wireState.`is`(block)) {
                val adjacentMask = connections.getWireBit(tsid, tdir.opposite)
                val adjTe = RedstoneTrackBlock.tile(level, wirePos).orElse(null)
                if (adjTe == null || (adjTe.getStateFlags() and adjacentMask) != adjacentMask) {
                    diagonalCheck = true
                } else {
                    positions.add(wirePos)
                    extSides.add(tsid)
                    intSides.add(side)
                    pwrSides.add(tdir)
                    trackConnectionUpdates.add(adjTe)
                    continue
                }
            }
            if (!diagonalCheck && wireState.`is`(Blocks.REDSTONE_WIRE)) {
                if (side != Direction.DOWN) {
                    diagonalCheck = true
                } else {
                    positions.add(wirePos)
                    extSides.add(tdir.opposite)
                    intSides.add(side)
                    pwrSides.add(tdir)
                    continue
                }
            }
            if (!diagonalCheck && wireState.isSignalSource) {
                positions.add(wirePos)
                extSides.add(tdir.opposite)
                intSides.add(side)
                pwrSides.add(tdir)
                continue
            }
            run {
                val trackPos = wirePos.relative(tsid)
                val trackState = level.getBlockState(trackPos)
                if (!trackState.`is`(block)) return@run
                val adjacentMask = connections.getWireBit(tdir.opposite, tsid.opposite)
                val adjTe = RedstoneTrackBlock.tile(level, trackPos).orElse(null)
                if (adjTe == null || (adjTe.getStateFlags() and adjacentMask) != adjacentMask) {
                    return@run
                }
                positions.add(trackPos)
                extSides.add(tdir.opposite)
                intSides.add(side)
                pwrSides.add(tdir)
                trackConnectionUpdates.add(adjTe)
            }
            if (!isRedstoneInsulator(wireState)) {
                positions.add(wirePos)
                extSides.add(tdir.opposite)
                intSides.add(side)
                pwrSides.add(tdir)
            }
        }
    }

    // Power bits for sides that are no longer part of any net must be explicitly cleared;
    // they are not overwritten by net construction and would otherwise carry stale values.
    private fun zeroUnusedSidePowers(usedSides: Set<Direction>): TrackStateFlags =
        Direction.entries
            .filter { !usedSides.contains(it) }
            .fold(stateFlags) { flags, side -> flags.withSidePower(side, 0) }

    /**
     * Finds adjacent track block entities that were connected before but are no longer reachable,
     * and adds them to [trackConnectionUpdates] so they recalculate and drop stale power.
     *
     * A track that was in the previous net's neighbourhood but is absent from every new net will
     * not receive a natural `neighborChanged` event — this block still exists, so Minecraft sees no
     * block-type change here. Without this step the disconnected track holds stale power.
     *
     * @param newNets The nets produced by [buildNets].
     * @param allNeighbours All neighbour positions from the previous nets, produced by
     *   [snapshotPreviousState].
     * @param trackConnectionUpdates Accumulator; disconnected adjacent track entities are added
     *   here.
     */
    private fun collectDisconnectedTrackUpdates(
        newNets: List<TrackNet>,
        allNeighbours: Set<BlockPos>,
        trackConnectionUpdates: MutableSet<TrackBlockEntity>,
    ) {
        val disconnectedNeighbours = HashSet<BlockPos>(allNeighbours)
        val connectedNeighbours = HashSet<BlockPos>()
        newNets.forEach { net ->
            net.neighbour_positions.forEach { disconnectedNeighbours.remove(it) }
        }
        newNets.forEach { net -> connectedNeighbours.addAll(net.neighbour_positions) }
        allNeighbours.forEach { connectedNeighbours.remove(it) }
        if (trace) {
            val poss = TrackBlockEntity.posstr(pos)
            for (net in newNets) {
                val adjacentDesc =
                    (0 until net.neighbour_positions.size).map { i ->
                        "${TrackBlockEntity.posstr(net.neighbour_positions[i])}:${net.neighbour_sides[i]}"
                    }
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "UCON: %s adj:%s | ints:%s | pwrs:%s",
                        poss,
                        adjacentDesc.joinToString(", "),
                        net.internal_sides.joinToString(",") { it.toString() },
                        net.power_sides.joinToString(",") { it.toString() },
                    )
                )
            }
            if (disconnectedNeighbours.isNotEmpty()) {
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "UCON: %s DISCONNECTED NEIGHBOURS: %s",
                        TrackBlockEntity.posstr(pos),
                        disconnectedNeighbours.joinToString(",") { TrackBlockEntity.posstr(it) },
                    )
                )
            }
            if (connectedNeighbours.isNotEmpty()) {
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "UCON: %s CONNECTED NEIGHBOURS: %s",
                        TrackBlockEntity.posstr(pos),
                        connectedNeighbours.joinToString(",") { TrackBlockEntity.posstr(it) },
                    )
                )
            }
        }
        HashSet(disconnectedNeighbours).forEach { p ->
            RedstoneTrackBlock.tile(level, p).ifPresent { te ->
                trackConnectionUpdates.add(te)
                disconnectedNeighbours.remove(p)
            }
        }
        if (trace && disconnectedNeighbours.isNotEmpty()) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "UCON: %s DISCONNECTED NONTRACK: %s",
                    TrackBlockEntity.posstr(pos),
                    disconnectedNeighbours.joinToString(",") { TrackBlockEntity.posstr(it) },
                )
            )
        }
    }

    // Intentionally narrow: only the two blocks that have historically broken track connectivity.
    private fun isRedstoneInsulator(state: BlockState): Boolean =
        state.`is`(Blocks.GLASS) || state.`is`(Blocks.AIR)
}
