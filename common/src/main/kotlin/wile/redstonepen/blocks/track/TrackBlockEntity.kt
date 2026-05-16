package wile.redstonepen.blocks.track

import java.util.LinkedList
import java.util.Locale
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.Tag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.jetbrains.annotations.Nullable
import wile.redstonepen.ModContent
import wile.redstonepen.blocks.StandardEntityBlocks
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections
import wile.redstonepen.items.RedstonePenItem
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries
import wile.redstonepen.util.RsSignals

@Suppress("DEPRECATION")
class TrackBlockEntity(pos: BlockPos, state: BlockState) :
    StandardEntityBlocks.StandardBlockEntity(
        Registries.getBlockEntityTypeOfBlock(state.block)!!,
        pos,
        state,
    ),
    Networking.IPacketTileNotifyReceiver {
    private var state_flags_: TrackStateFlags = TrackStateFlags.EMPTY
    private val nets_: MutableList<TrackNet> = ArrayList()
    private val block_change_tracking_: Array<Block> = Array(6) { Blocks.AIR }
    private var trace_: Boolean = false

    companion object {
        @JvmStatic internal fun posstr(pos: BlockPos): String = "[${pos.x},${pos.y},${pos.z}]"

        @JvmStatic
        internal fun dirstr(@Nullable dir: Direction?): String =
            if (dir == null) "?" else dir.toString().substring(0, 1)

        private val updatepower_order: List<Vec3i> by lazy {
            val list = ArrayList<Vec3i>()
            for (side in Direction.values()) {
                list.add(Vec3i(0, 0, 0).relative(side, 1))
            }
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 2) list.add(Vec3i(x, y, z))
                    }
                }
            }
            list
        }
    }

    override fun readnbt(hlp: HolderLookup.Provider, nbt: CompoundTag): CompoundTag {
        state_flags_ = TrackStateFlags(nbt.getLong("sflags"))
        nets_.clear()
        if (nbt.contains("nets", Tag.TAG_LIST.toInt())) {
            val lst = nbt.getList("nets", Tag.TAG_COMPOUND.toInt())
            try {
                for (i in 0 until lst.size) {
                    val route_nbt = lst.getCompound(i)
                    nets_.add(
                        TrackNet(
                            route_nbt.getLongArray("npos").map { BlockPos.of(it) },
                            route_nbt.getIntArray("nsid").map { Direction.from3DDataValue(it) },
                            route_nbt.getIntArray("ifac").map { Direction.from3DDataValue(it) },
                            route_nbt.getIntArray("pfac").map { Direction.from3DDataValue(it) },
                            route_nbt.getInt("power"),
                        )
                    )
                }
            } catch (ex: Throwable) {
                nets_.clear()
                Auxiliaries.logError(
                    "Dropped invalid NBT for Redstone Track at pos $blockPos ($ex)"
                )
            }
        }
        return nbt
    }

    override fun writenbt(
        hlp: HolderLookup.Provider,
        nbt: CompoundTag,
        syncPacket: Boolean,
    ): CompoundTag {
        nbt.putLong("sflags", state_flags_.raw)
        if (syncPacket) return nbt
        if (nets_.isNotEmpty()) {
            val lst = ListTag()
            for (net in nets_) {
                val route_nbt = CompoundTag()
                route_nbt.putInt("power", net.power)
                route_nbt.put(
                    "npos",
                    LongArrayTag(net.neighbour_positions.map { it.asLong() }.toLongArray()),
                )
                route_nbt.put(
                    "nsid",
                    IntArrayTag(net.neighbour_sides.map { it.get3DDataValue() }.toIntArray()),
                )
                route_nbt.put(
                    "ifac",
                    IntArrayTag(net.internal_sides.map { it.get3DDataValue() }.toIntArray()),
                )
                route_nbt.put(
                    "pfac",
                    IntArrayTag(net.power_sides.map { it.get3DDataValue() }.toIntArray()),
                )
                lst.add(route_nbt)
            }
            nbt.put("nets", lst)
        }
        return nbt
    }

    override fun onServerPacketReceived(nbt: CompoundTag) {
        readnbt(getLevel()!!.registryAccess(), nbt)
    }

    override fun onClientPacketReceived(player: Player, nbt: CompoundTag) {}

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    @Suppress("FunctionOnlyReturningConstant")
    @Environment(EnvType.CLIENT)
    fun getViewDistance(): Double = 64.0

    fun sync(schedule: Boolean): Boolean {
        if (level!!.isClientSide()) return true
        setChanged()
        if (
            schedule &&
                !getLevel()!!
                    .getBlockTicks()
                    .hasScheduledTick(blockPos, ModContent.References.TRACK_BLOCK)
        ) {
            getLevel()!!.scheduleTick(blockPos, ModContent.References.TRACK_BLOCK, 1)
        } else {
            Networking.PacketTileNotifyServerToClient.sendToPlayers(
                this,
                writenbt(getLevel()!!.registryAccess(), CompoundTag(), true),
            )
        }
        return true
    }

    fun getStateFlags(): Long = state_flags_.raw

    fun addWireFlags(flags: Long): Int {
        val (newFlags, added) = state_flags_.withAddedWireFlags(flags)
        state_flags_ = newFlags
        return added
    }

    fun getWireFlags(): Int = state_flags_.wireFlags

    fun getWireFlag(index: Int): Boolean = state_flags_.wireFlag(index)

    fun getWireFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_WIR_COUNT

    fun getConnectionFlags(): Int = state_flags_.connectionFlags

    fun getConnectionFlag(index: Int): Boolean = state_flags_.connectionFlag(index)

    fun getConnectionFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_CON_COUNT

    fun getSidePower(side: Direction): Int = state_flags_.sidePower(side)

    fun setSidePower(side: Direction, p: Int) {
        state_flags_ = state_flags_.withSidePower(side, p)
    }

    fun hasVanillaRedstoneConnection(side: Direction): Boolean =
        RedstoneTrackDefs.connections.hasVanillaWireConnection(getStateFlags(), side) ||
            state_flags_.hasBits(RedstoneTrackDefs.connections.getBulkConnectorBit(side))

    fun getRedstonePower(redstone_side: Direction, _weak: Boolean): Int {
        if (isRemoved) return 0
        val own_side = redstone_side.opposite
        var p = 0
        for (net in nets_) {
            if (!net.power_sides.contains(own_side)) continue
            p = maxOf(p, net.power)
            if (p >= 15) break
        }
        p =
            if (
                p <= 0 ||
                    !getLevel()!!
                        .getBlockState(blockPos.relative(own_side))
                        .`is`(Blocks.REDSTONE_WIRE)
            ) {
                p
            } else {
                p - 1
            }
        return p
    }

    fun getRedstoneDustCount(): Int {
        var n = 0
        var rem = getWireFlags()
        var i = 0
        while (rem != 0 && i < RedstoneTrackDefs.STATE_FLAG_WIR_COUNT) {
            if ((rem and 1) != 0) ++n
            rem = rem shr 1
            ++i
        }
        rem = getConnectionFlags()
        i = 0
        while (rem != 0 && i < RedstoneTrackDefs.STATE_FLAG_CON_COUNT) {
            if ((rem and 1) != 0) ++n
            rem = rem shr 1
            ++i
        }
        return n
    }

    fun toggle_trace(@Nullable player: Player?) {
        if (!Auxiliaries.isDevelopmentMode()) {
            trace_ = false
            if (player != null) {
                Auxiliaries.playerChatMessage(player, "Trace disabled, not in development mode.")
            }
        } else {
            trace_ = !trace_
            if (player != null) Auxiliaries.playerChatMessage(player, "Trace: $trace_")
        }
    }

    internal fun modifySegments(
        pos: BlockPos,
        player: Player,
        used_stack: ItemStack,
        clicked_face: Direction,
        hitvec: Vec3,
        no_add: Boolean,
        no_remove: Boolean,
        no_bulk: Boolean,
    ): Int {
        if (
            !used_stack.isEmpty &&
                used_stack.item != Items.REDSTONE &&
                !RedstonePenItem.isPen(used_stack)
        ) {
            return 0
        }
        val face = clicked_face.opposite
        val flip_mask = computeFlipMask(clicked_face, face, hitvec, no_add, no_bulk)
        val material_use = applyWireMutation(flip_mask, face, no_add, no_remove, player, used_stack)
        if (material_use != 0) {
            propagateEdit(face, pos, getSidePower(face))
            sync(true)
        }
        return material_use
    }

    private fun computeFlipMask(
        clickedFace: Direction,
        face: Direction,
        hitvec: Vec3,
        noAdd: Boolean,
        noBulk: Boolean,
    ): Long {
        val hitR = hitvec.subtract(Vec3.atCenterOf(blockPos))
        val hit =
            when (clickedFace) {
                Direction.WEST,
                Direction.EAST -> hitR.multiply(0.0, 1.0, 1.0)
                Direction.SOUTH,
                Direction.NORTH -> hitR.multiply(1.0, 1.0, 0.0)
                else -> hitR.multiply(1.0, 0.0, 1.0)
            }
        val dir = Direction.getNearest(hit.x(), hit.y(), hit.z())
        val faceIsEmpty =
            (getWireFlags() and RedstoneTrackDefs.connections.getAllElementsOnFace(face).toInt()) ==
                0
        val shouldBulkConnect =
            !noBulk && !faceIsEmpty && hit.length() < 0.10 && (!noAdd || getConnectionFlags() != 0)
        return if (shouldBulkConnect) {
            RedstoneTrackDefs.connections.getBulkConnectorBit(face)
        } else if (noAdd || hit.length() > 0.11) {
            var m = RedstoneTrackDefs.connections.getWireBit(face, dir)
            if (!noAdd) {
                if (isAdjacentWireSegmentAddable(face, dir.opposite)) {
                    m = m or RedstoneTrackDefs.connections.getWireBit(face, dir.opposite)
                }
                if (isAdjacentWireSegmentAddable(face, dir.getClockWise(face.axis))) {
                    m =
                        m or
                            RedstoneTrackDefs.connections.getWireBit(
                                face,
                                dir.getClockWise(face.axis),
                            )
                }
                if (isAdjacentWireSegmentAddable(face, dir.getCounterClockWise(face.axis))) {
                    m =
                        m or
                            RedstoneTrackDefs.connections.getWireBit(
                                face,
                                dir.getCounterClockWise(face.axis),
                            )
                }
            }
            m
        } else {
            0L
        }
    }

    private fun applyWireMutation(
        flipMask: Long,
        face: Direction,
        noAdd: Boolean,
        noRemove: Boolean,
        player: Player,
        usedStack: ItemStack,
    ): Int {
        var materialUse = 0
        if (state_flags_.hasBits(flipMask)) {
            if (!noRemove) {
                state_flags_ = state_flags_.withBitsCleared(flipMask)
                materialUse -= 1
                val bc = RedstoneTrackDefs.connections.getBulkConnectorBit(face)
                if (
                    (state_flags_.raw and
                        RedstoneTrackDefs.connections.getAllElementsOnFace(face)) == bc
                ) {
                    state_flags_ = state_flags_.withBitsCleared(bc)
                    materialUse -= 1
                }
                if (getWireFlags() == 0) {
                    materialUse -= getRedstoneDustCount()
                    state_flags_ = TrackStateFlags.EMPTY
                }
            }
        } else if (!noAdd) {
            for (i in 0 until RedstoneTrackDefs.STATE_FLAG_PWR_POS) {
                val mask = 1L shl i
                if ((flipMask and mask) == 0L || (getStateFlags() and mask) != 0L) continue
                if (!RedstonePenItem.hasEnoughRedstone(usedStack, materialUse + 1, player)) break
                state_flags_ = state_flags_.withBitsSet(mask)
                materialUse += 1
            }
        }
        return materialUse
    }

    private fun propagateEdit(face: Direction, originalPos: BlockPos, initialSidePower: Int) {
        setSidePower(face, 0)
        val changesBefore = updateAllPowerValuesFromAdjacent()
        val netNeighboursBefore: Set<BlockPos> =
            nets_
                .firstOrNull { it.internal_sides.contains(face) }
                ?.neighbour_positions
                ?.let { HashSet(it) } ?: HashSet()
        updateConnections(1)
        setSidePower(face, 0)
        val changesAfter = updateAllPowerValuesFromAdjacent()
        val disconnected =
            changesBefore.keys.filter { !changesAfter.containsKey(it) }.toMutableList()
        val connected = changesAfter.keys.filter { !changesBefore.containsKey(it) }.toMutableList()
        if (
            connected.isEmpty() &&
                disconnected.isEmpty() &&
                RedstoneTrackDefs.connections.hasBulkConnection(getStateFlags(), face)
        ) {
            val netNeighboursAfter: Set<BlockPos> =
                nets_
                    .firstOrNull { it.internal_sides.contains(face) }
                    ?.neighbour_positions
                    ?.let { HashSet(it) } ?: HashSet()
            for (p in netNeighboursAfter) {
                if (!netNeighboursBefore.contains(p)) connected.add(p)
            }
            for (p in netNeighboursBefore) {
                if (!netNeighboursAfter.contains(p)) disconnected.add(p)
            }
        }
        if (connected.isEmpty() && disconnected.isEmpty()) {
            setSidePower(face, initialSidePower)
        } else {
            setSidePower(face, 0)
            nets_.forEach { net -> if (net.internal_sides.contains(face)) net.power = 0 }
            disconnected.forEach { p ->
                val te = getLevel()!!.getBlockEntity(p)
                getLevel()!!
                    .getBlockState(p)
                    .handleNeighborChanged(getLevel()!!, p, getBlock(), originalPos, false)
                if (te is TrackBlockEntity) te.updateConnections(1)
            }
            connected.forEach { p ->
                val te = getLevel()!!.getBlockEntity(p)
                if (te is TrackBlockEntity) te.updateConnections(1)
                getLevel()!!
                    .getBlockState(p)
                    .handleNeighborChanged(getLevel()!!, p, getBlock(), originalPos, false)
                getBlock().neighborChanged(blockState, getLevel()!!, blockPos, getBlock(), p, false)
            }
        }
    }

    private fun isAdjacentWireSegmentAddable(face: Direction, dir: Direction): Boolean {
        if ((getStateFlags() and RedstoneTrackDefs.connections.getWireBit(face, dir)) != 0L) {
            return false
        }
        val pos = blockPos.relative(dir)
        val te = RedstoneTrackBlock.tile(getLevel()!!, pos).orElse(null)
        if (te != null) {
            return (te.getStateFlags() and
                RedstoneTrackDefs.connections.getWireBit(face, dir.opposite)) != 0L
        }
        val state = getLevel()!!.getBlockState(pos)
        return state.`is`(Blocks.REDSTONE_WIRE) || state.isSignalSource
    }

    fun updateAllPowerValuesFromAdjacent(): Map<BlockPos, BlockPos> {
        val all_change_notifications = HashMap<BlockPos, BlockPos>()
        for (ofs in updatepower_order) {
            handleNeighborChanged(blockPos.offset(ofs))
                .forEach(all_change_notifications::putIfAbsent)
        }
        return all_change_notifications
    }

    private fun spawnRedstoneItems(count: Int) {
        if (count <= 0) return
        val e =
            ItemEntity(
                getLevel()!!,
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5,
                ItemStack(Items.REDSTONE, count),
            )
        e.setDefaultPickUpDelay()
        e.setDeltaMovement(
            Vec3(
                    getLevel()!!.random.nextDouble() - .5,
                    getLevel()!!.random.nextDouble() - .5,
                    getLevel()!!.random.nextDouble(),
                )
                .scale(0.1)
        )
        getLevel()!!.addFreshEntity(e)
    }

    private fun getBlock(): RedstoneTrackBlock = ModContent.References.TRACK_BLOCK

    fun handleShapeUpdate(
        facing: Direction,
        facingState: BlockState,
        fromPos: BlockPos,
        isMoving: Boolean,
    ): Boolean {
        var update_neighbours = false
        if (
            !RedstoneTrackBlock.canBePlacedOnFace(
                facingState,
                getLevel()!!,
                fromPos,
                facing.opposite,
            )
        ) {
            val to_remove = RedstoneTrackDefs.connections.getAllElementsOnFace(facing)
            val new_flags = state_flags_.withBitsCleared(to_remove)
            if (new_flags != state_flags_) {
                if (trace_) {
                    Auxiliaries.logWarn(
                        String.format(
                            Locale.ROOT,
                            "SHUP: %s <-%s(=%s) removed.",
                            posstr(blockPos),
                            posstr(fromPos),
                            facingState.block.descriptionId,
                        )
                    )
                }
                var count = getRedstoneDustCount()
                state_flags_ = new_flags
                count -= getRedstoneDustCount()
                spawnRedstoneItems(count)
                updateConnections(1)
                update_neighbours = true
            }
        }
        var bltv: Block = block_change_tracking_[facing.get3DDataValue()]
        if (bltv != facingState.block) {
            if (trace_) {
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "SHUP: %s <-%s changed (%s->%s).",
                        posstr(blockPos),
                        posstr(fromPos),
                        bltv.descriptionId,
                        facingState.block.descriptionId,
                    )
                )
            }
            block_change_tracking_[facing.get3DDataValue()] = facingState.block
            if (!isMoving && bltv != Blocks.REDSTONE_BLOCK) updateConnections(1)
            update_neighbours = true
        }
        if (update_neighbours) {
            val world = getLevel()!!
            val block = getBlock()
            handleNeighborChanged(fromPos).forEach { chpos, frpos ->
                world.neighborChanged(chpos, block, frpos)
            }
        }
        return getWireFlags() != 0
    }

    private fun getNonWireSignal(world: Level, pos: BlockPos, redstone_side: Direction): Int {
        getBlock().disablePower(true)
        val state = world.getBlockState(pos)
        var p =
            if (!state.`is`(Blocks.REDSTONE_WIRE) && !state.`is`(getBlock())) {
                state.getSignal(world, pos, redstone_side)
            } else {
                0
            }
        if (!RsSignals.canEmitWeakPower(state, world, pos, redstone_side)) {
            getBlock().disablePower(false)
            return p
        }
        for (rs_side in Direction.values()) {
            val side_pos = pos.relative(rs_side)
            val side_state = world.getBlockState(side_pos)
            if (side_state.`is`(Blocks.REDSTONE_WIRE) || side_state.`is`(getBlock())) continue
            val p_in = side_state.getDirectSignal(world, side_pos, rs_side)
            if (p_in > p) {
                p = p_in
                if (p >= 15) break
            }
        }
        getBlock().disablePower(false)
        return p
    }

    private fun isNetConnectedTo(
        _pos: BlockPos,
        net: TrackNet,
        otherPos: BlockPos,
        @Nullable otherSide: Direction?,
        @Nullable otherNet: TrackNet?,
    ): Boolean {
        if (otherNet == null) return net.neighbour_positions.any { it == otherPos }
        for (i in 0 until net.neighbour_positions.size) {
            if (net.neighbour_positions[i] != otherPos) continue
            val nb_side = net.neighbour_sides[i]
            if (otherSide != null && otherSide != nb_side) continue
            if (!otherNet.internal_sides.contains(nb_side)) continue
            return true
        }
        return false
    }

    fun handleNeighborChanged(fromPos: BlockPos): Map<BlockPos, BlockPos> {
        val notifications = LinkedHashMap<BlockPos, BlockPos>()
        nets_
            .filter { it.neighbour_positions.contains(fromPos) }
            .forEach { net -> handleNetNeighborChanged(net, fromPos, null, notifications) }
        val fst = getLevel()!!.getBlockState(fromPos)
        if (fst.`is`(getBlock()) || fst.isSignalSource) notifications.remove(fromPos)
        if (trace_ && notifications.isNotEmpty()) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "NBCH: %s updates: [%s]",
                    posstr(blockPos),
                    notifications.entries.joinToString(", ") {
                        "${posstr(it.value)}>${posstr(it.key)}"
                    },
                )
            )
        }
        return notifications
    }

    fun handleNetNeighborChanged(
        net: TrackNet,
        fromPos: BlockPos,
        @Nullable fromNet: TrackNet?,
        @Nullable change_notifications: MutableMap<BlockPos, BlockPos>?,
    ) {
        data class Neighbor(
            val pos: BlockPos,
            val side: Direction,
            val power: Int,
            val direct_update: Boolean,
            val needs_indirect: Boolean,
        )

        val my_pos = blockPos
        if (!isNetConnectedTo(my_pos, net, fromPos, null, fromNet)) return
        val world = getLevel()!!
        val neighbors = LinkedList<Neighbor>()
        if (trace_) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "NBCH: %s from %s (%s)",
                    posstr(my_pos),
                    posstr(fromPos),
                    world.getBlockState(fromPos).block.descriptionId,
                )
            )
        }
        var pmax = 0
        for (i in 0 until net.neighbour_positions.size) {
            val ext_pos = net.neighbour_positions[i]
            val ext_side = net.neighbour_sides[i]
            val ext_state = level!!.getBlockState(ext_pos)
            if (ext_state.`is`(Blocks.REDSTONE_WIRE)) {
                val p_vanilla_wire = ext_state.getValue(RedStoneWireBlock.POWER)
                neighbors.add(Neighbor(ext_pos, ext_side, p_vanilla_wire, false, false))
                pmax = maxOf(pmax, p_vanilla_wire - 1)
            } else if (ext_state.`is`(getBlock())) {
                val nb_net =
                    RedstoneTrackBlock.tile(world, ext_pos)
                        .flatMap { te ->
                            te.nets_
                                .stream()
                                .filter { nbn ->
                                    isNetConnectedTo(my_pos, net, ext_pos, ext_side, nbn)
                                }
                                .findFirst()
                        }
                        .orElse(null)
                if (nb_net != null) {
                    val p_track = maxOf(0, nb_net.power)
                    neighbors.add(Neighbor(ext_pos, ext_side, p_track, true, false))
                    pmax = maxOf(pmax, p_track - 1)
                }
            } else if (ext_state.`is`(ModContent.References.BRIDGE_RELAY_BLOCK)) {
                val p_nowire = getNonWireSignal(world, ext_pos, ext_side.opposite)
                neighbors.add(Neighbor(ext_pos, ext_side, p_nowire, true, false))
                pmax = maxOf(pmax, p_nowire)
            } else {
                val p_nowire = getNonWireSignal(world, ext_pos, ext_side.opposite)
                val weak_updates =
                    !ext_state.isSignalSource &&
                        p_nowire == 0 &&
                        ext_state.isRedstoneConductor(world, ext_pos)
                neighbors.add(Neighbor(ext_pos, ext_side, p_nowire, false, weak_updates))
                pmax = maxOf(pmax, p_nowire)
            }
        }
        var power_changed = false
        if (net.power != pmax) {
            if (trace_) {
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "NBCH: %s net power %d->%d",
                        posstr(my_pos),
                        net.power,
                        pmax,
                    )
                )
            }
            net.power = pmax
            power_changed = true
        }
        for (side in net.internal_sides) {
            if (getSidePower(side) != pmax) {
                setSidePower(side, pmax)
                power_changed = true
            }
        }
        if (!power_changed) return
        for (neighbor in neighbors) {
            if (neighbor.direct_update) {
                val be = world.getBlockEntity(neighbor.pos)
                if (be is TrackBlockEntity) {
                    for (nb_net in be.nets_) {
                        be.handleNetNeighborChanged(nb_net, my_pos, net, change_notifications)
                    }
                } else {
                    world
                        .getBlockState(neighbor.pos)
                        .handleNeighborChanged(world, neighbor.pos, getBlock(), my_pos, false)
                }
            } else {
                change_notifications?.putIfAbsent(neighbor.pos, my_pos)
                if (neighbor.needs_indirect) {
                    for (update_direction in RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS) {
                        if (neighbor.side == update_direction) continue
                        change_notifications?.putIfAbsent(
                            neighbor.pos.relative(update_direction),
                            neighbor.pos,
                        )
                    }
                }
            }
        }
        sync(true)
    }

    internal fun updateConnections(recursion_left: Int) {
        val result =
            TrackNetworkCalculator(getLevel()!!, blockPos, state_flags_, getBlock(), trace_)
                .calculate(nets_)
        state_flags_ = result.newStateFlags
        nets_.clear()
        nets_.addAll(result.nets)
        setChanged()
        if (recursion_left > 0) {
            for (te in result.trackConnectionUpdates) {
                if (trace_) {
                    Auxiliaries.logWarn(
                        String.format(
                            Locale.ROOT,
                            "UCON: %s UPDATE NET OF %s",
                            posstr(blockPos),
                            posstr(te.blockPos),
                        )
                    )
                }
                te.updateConnections(recursion_left - 1)
            }
        }
        val world = getLevel()!!
        val state = blockState
        result.neighboursToNotify.forEach { pos ->
            val st = world.getBlockState(pos)
            if (trace_) {
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "UCON: %s UPDATE TRACK CHANGES TO %s.",
                        posstr(blockPos),
                        posstr(pos),
                    )
                )
            }
            st.handleNeighborChanged(world, pos, state.block, blockPos, false)
            world.updateNeighborsAt(pos, st.block)
        }
    }
}
