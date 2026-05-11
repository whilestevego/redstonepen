package wile.redstonepen.blocks.track

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
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.jetbrains.annotations.Nullable
import java.util.LinkedList
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
    StandardEntityBlocks.StandardBlockEntity(Registries.getBlockEntityTypeOfBlock(state.block)!!, pos, state),
    Networking.IPacketTileNotifyReceiver
{
    private var state_flags_: Long = 0
    private val nets_: MutableList<TrackNet> = ArrayList()
    private val block_change_tracking_: Array<Block> = Array(6) { Blocks.AIR }
    private var trace_: Boolean = false

    companion object {
        @JvmStatic internal fun posstr(pos: BlockPos): String = "[${pos.x},${pos.y},${pos.z}]"

        @JvmStatic internal fun dirstr(@Nullable dir: Direction?): String =
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
        state_flags_ = nbt.getLong("sflags")
        nets_.clear()
        if (nbt.contains("nets", Tag.TAG_LIST.toInt())) {
            val lst = nbt.getList("nets", Tag.TAG_COMPOUND.toInt())
            try {
                for (i in 0 until lst.size) {
                    val route_nbt = lst.getCompound(i)
                    nets_.add(TrackNet(
                        route_nbt.getLongArray("npos").map { BlockPos.of(it) },
                        route_nbt.getIntArray("nsid").map { Direction.from3DDataValue(it) },
                        route_nbt.getIntArray("ifac").map { Direction.from3DDataValue(it) },
                        route_nbt.getIntArray("pfac").map { Direction.from3DDataValue(it) },
                        route_nbt.getInt("power")
                    ))
                }
            } catch (ex: Throwable) {
                nets_.clear()
                Auxiliaries.logError("Dropped invalid NBT for Redstone Track at pos $blockPos")
            }
        }
        return nbt
    }

    override fun writenbt(hlp: HolderLookup.Provider, nbt: CompoundTag, sync_packet: Boolean): CompoundTag {
        nbt.putLong("sflags", state_flags_)
        if (sync_packet) return nbt
        if (nets_.isNotEmpty()) {
            val lst = ListTag()
            for (net in nets_) {
                val route_nbt = CompoundTag()
                route_nbt.putInt("power", net.power)
                route_nbt.put("npos", LongArrayTag(net.neighbour_positions.map { it.asLong() }.toLongArray()))
                route_nbt.put("nsid", IntArrayTag(net.neighbour_sides.map { it.get3DDataValue() }.toIntArray()))
                route_nbt.put("ifac", IntArrayTag(net.internal_sides.map { it.get3DDataValue() }.toIntArray()))
                route_nbt.put("pfac", IntArrayTag(net.power_sides.map { it.get3DDataValue() }.toIntArray()))
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

    @Environment(EnvType.CLIENT)
    fun getViewDistance(): Double = 64.0

    fun sync(schedule: Boolean): Boolean {
        if (level!!.isClientSide()) return true
        setChanged()
        if (schedule && !getLevel()!!.getBlockTicks().hasScheduledTick(blockPos, ModContent.references.TRACK_BLOCK)) {
            getLevel()!!.scheduleTick(blockPos, ModContent.references.TRACK_BLOCK, 1)
        } else {
            Networking.PacketTileNotifyServerToClient.sendToPlayers(this, writenbt(getLevel()!!.registryAccess(), CompoundTag(), true))
        }
        return true
    }

    fun getStateFlags(): Long = state_flags_

    fun addWireFlags(flags: Long): Int {
        var n_added = 0
        for (i in 0 until getWireFlagCount()) {
            val mask = 1L shl i
            if ((flags and mask) != 0L && (state_flags_ and mask) == 0L) {
                state_flags_ = state_flags_ or mask
                ++n_added
            }
        }
        return n_added
    }

    fun getWireFlags(): Int =
        ((state_flags_ and RedstoneTrackDefs.STATE_FLAG_WIR_MASK) shr RedstoneTrackDefs.STATE_FLAG_WIR_POS).toInt()

    fun getWireFlag(index: Int): Boolean =
        (state_flags_ and (1L shl (RedstoneTrackDefs.STATE_FLAG_WIR_POS + index))) != 0L

    fun getWireFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_WIR_COUNT

    fun getConnectionFlags(): Int =
        ((state_flags_ and RedstoneTrackDefs.STATE_FLAG_CON_MASK) shr RedstoneTrackDefs.STATE_FLAG_CON_POS).toInt()

    fun getConnectionFlag(index: Int): Boolean =
        (state_flags_ and (1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + index))) != 0L

    fun getConnectionFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_CON_COUNT

    fun getSidePower(side: Direction): Int {
        val shift = RedstoneTrackDefs.STATE_FLAG_PWR_POS + 4 * (connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0))
        return ((state_flags_ shr shift) and 0xfL).toInt()
    }

    fun setSidePower(side: Direction, p: Int) {
        val shift = RedstoneTrackDefs.STATE_FLAG_PWR_POS + 4 * (connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0))
        state_flags_ = (state_flags_ and (0xfL shl shift).inv()) or ((p.toLong() and 0xfL) shl shift)
    }

    fun hasVanillaRedstoneConnection(side: Direction): Boolean =
        RedstoneTrackDefs.connections.hasVanillaWireConnection(getStateFlags(), side) ||
            (state_flags_ and RedstoneTrackDefs.connections.getBulkConnectorBit(side)) != 0L

    fun getRedstonePower(redstone_side: Direction, weak: Boolean): Int {
        if (isRemoved) return 0
        val own_side = redstone_side.opposite
        var p = 0
        for (net in nets_) {
            if (!net.power_sides.contains(own_side)) continue
            p = maxOf(p, net.power)
            if (p >= 15) break
        }
        p = if (p <= 0 || !getLevel()!!.getBlockState(blockPos.relative(own_side)).`is`(Blocks.REDSTONE_WIRE)) p else p - 1
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
            if (player != null) Auxiliaries.playerChatMessage(player, "Trace disabled, not in development mode.")
        } else {
            trace_ = !trace_
            if (player != null) Auxiliaries.playerChatMessage(player, "Trace: $trace_")
        }
    }

    internal fun modifySegments(pos: BlockPos, player: Player, used_stack: ItemStack, clicked_face: Direction, hitvec: Vec3, no_add: Boolean, no_remove: Boolean, no_bulk: Boolean): Int {
        if (!used_stack.isEmpty && used_stack.item != Items.REDSTONE && !RedstonePenItem.isPen(used_stack)) return 0
        val flip_mask: Long
        val face = clicked_face.opposite
        run {
            val hit_r = hitvec.subtract(Vec3.atCenterOf(pos))
            val hit = when (clicked_face) {
                Direction.WEST, Direction.EAST   -> hit_r.multiply(0.0, 1.0, 1.0)
                Direction.SOUTH, Direction.NORTH -> hit_r.multiply(1.0, 1.0, 0.0)
                else                             -> hit_r.multiply(1.0, 0.0, 1.0)
            }
            val dir = Direction.getNearest(hit.x(), hit.y(), hit.z())
            val face_is_empty = (getWireFlags() and RedstoneTrackDefs.connections.getAllElementsOnFace(face).toInt()) == 0
            flip_mask = if (!no_bulk && !face_is_empty && hit.length() < 0.10 && (!no_add || getConnectionFlags() != 0)) {
                RedstoneTrackDefs.connections.getBulkConnectorBit(face)
            } else if (no_add || hit.length() > 0.11) {
                var m = RedstoneTrackDefs.connections.getWireBit(face, dir)
                if (!no_add) {
                    if (isAdjacentWireSegmentAddable(face, dir.opposite)) m = m or RedstoneTrackDefs.connections.getWireBit(face, dir.opposite)
                    if (isAdjacentWireSegmentAddable(face, dir.getClockWise(face.axis))) m = m or RedstoneTrackDefs.connections.getWireBit(face, dir.getClockWise(face.axis))
                    if (isAdjacentWireSegmentAddable(face, dir.getCounterClockWise(face.axis))) m = m or RedstoneTrackDefs.connections.getWireBit(face, dir.getCounterClockWise(face.axis))
                }
                m
            } else {
                0L
            }
        }
        var material_use = 0
        run {
            if ((state_flags_ and flip_mask) != 0L) {
                if (!no_remove) {
                    state_flags_ = state_flags_ and flip_mask.inv()
                    material_use -= 1
                    val bc = RedstoneTrackDefs.connections.getBulkConnectorBit(face)
                    if ((state_flags_ and RedstoneTrackDefs.connections.getAllElementsOnFace(face)) == bc) {
                        state_flags_ = state_flags_ and bc.inv()
                        material_use -= 1
                    }
                    if (getWireFlags() == 0) {
                        material_use -= getRedstoneDustCount()
                        state_flags_ = 0
                    }
                }
            } else if (!no_add) {
                for (i in 0 until RedstoneTrackDefs.STATE_FLAG_PWR_POS) {
                    val mask = 1L shl i
                    if ((flip_mask and mask) == 0L || (getStateFlags() and mask) != 0L) continue
                    if (!RedstonePenItem.hasEnoughRedstone(used_stack, material_use + 1, player)) break
                    state_flags_ = state_flags_ or mask
                    material_use += 1
                }
            }
        }
        if (material_use != 0) {
            val connected: MutableList<BlockPos>
            val disconnected: MutableList<BlockPos>
            val initial_side_power = getSidePower(face)
            run {
                setSidePower(face, 0)
                val change_notifications_before = updateAllPowerValuesFromAdjacent()
                val net_neighbours_before: Set<BlockPos> = nets_.firstOrNull { it.internal_sides.contains(face) }?.neighbour_positions?.let { HashSet(it) } ?: HashSet()
                updateConnections(1)
                setSidePower(face, 0)
                val change_notifications_after = updateAllPowerValuesFromAdjacent()
                disconnected = change_notifications_before.keys.filter { !change_notifications_after.containsKey(it) }.toMutableList()
                connected = change_notifications_after.keys.filter { !change_notifications_before.containsKey(it) }.toMutableList()
                if (connected.isEmpty() && disconnected.isEmpty() && RedstoneTrackDefs.connections.hasBulkConnection(getStateFlags(), face)) {
                    val net_neighbours_after: Set<BlockPos> = nets_.firstOrNull { it.internal_sides.contains(face) }?.neighbour_positions?.let { HashSet(it) } ?: HashSet()
                    for (p in net_neighbours_after) { if (!net_neighbours_before.contains(p)) connected.add(p) }
                    for (p in net_neighbours_before) { if (!net_neighbours_after.contains(p)) disconnected.add(p) }
                }
            }
            if (connected.isEmpty() && disconnected.isEmpty()) {
                setSidePower(face, initial_side_power)
            } else {
                setSidePower(face, 0)
                nets_.forEach { net -> if (net.internal_sides.contains(face)) net.power = 0 }
                disconnected.forEach { p ->
                    val te = getLevel()!!.getBlockEntity(p)
                    getLevel()!!.getBlockState(p).handleNeighborChanged(getLevel()!!, p, getBlock(), pos, false)
                    if (te is TrackBlockEntity) te.updateConnections(1)
                }
                connected.forEach { p ->
                    val te = getLevel()!!.getBlockEntity(p)
                    if (te is TrackBlockEntity) te.updateConnections(1)
                    getLevel()!!.getBlockState(p).handleNeighborChanged(getLevel()!!, p, getBlock(), pos, false)
                    getBlock().neighborChanged(blockState, getLevel()!!, blockPos, getBlock(), p, false)
                }
            }
            sync(true)
        }
        return material_use
    }

    private fun isAdjacentWireSegmentAddable(face: Direction, dir: Direction): Boolean {
        if ((getStateFlags() and RedstoneTrackDefs.connections.getWireBit(face, dir)) != 0L) return false
        val pos = blockPos.relative(dir)
        val te = RedstoneTrackBlock.tile(getLevel()!!, pos).orElse(null)
        if (te != null) return (te.getStateFlags() and RedstoneTrackDefs.connections.getWireBit(face, dir.opposite)) != 0L
        val state = getLevel()!!.getBlockState(pos)
        return state.`is`(Blocks.REDSTONE_WIRE) || state.isSignalSource
    }

    fun updateAllPowerValuesFromAdjacent(): Map<BlockPos, BlockPos> {
        val all_change_notifications = HashMap<BlockPos, BlockPos>()
        for (ofs in updatepower_order) {
            handleNeighborChanged(blockPos.offset(ofs)).forEach(all_change_notifications::putIfAbsent)
        }
        return all_change_notifications
    }

    private fun spawnRedstoneItems(count: Int) {
        if (count <= 0) return
        val e = ItemEntity(getLevel()!!, blockPos.x + .5, blockPos.y + .5, blockPos.z + .5, ItemStack(Items.REDSTONE, count))
        e.setDefaultPickUpDelay()
        e.setDeltaMovement(Vec3(getLevel()!!.random.nextDouble() - .5, getLevel()!!.random.nextDouble() - .5, getLevel()!!.random.nextDouble()).scale(0.1))
        getLevel()!!.addFreshEntity(e)
    }

    private fun getBlock(): RedstoneTrackBlock = ModContent.references.TRACK_BLOCK!!

    fun handleShapeUpdate(facing: Direction, facingState: BlockState, fromPos: BlockPos, isMoving: Boolean): Boolean {
        var update_neighbours = false
        if (!RedstoneTrackBlock.canBePlacedOnFace(facingState, getLevel()!!, fromPos, facing.opposite)) {
            val to_remove = RedstoneTrackDefs.connections.getAllElementsOnFace(facing)
            val new_flags = state_flags_ and to_remove.inv()
            if (new_flags != state_flags_) {
                if (trace_) Auxiliaries.logWarn(String.format("SHUP: %s <-%s(=%s) removed.", posstr(blockPos), posstr(fromPos), facingState.block.descriptionId))
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
            if (trace_) Auxiliaries.logWarn(String.format("SHUP: %s <-%s changed (%s->%s).", posstr(blockPos), posstr(fromPos), bltv.descriptionId, facingState.block.descriptionId))
            block_change_tracking_[facing.get3DDataValue()] = facingState.block
            if (!isMoving && bltv != Blocks.REDSTONE_BLOCK) updateConnections(1)
            update_neighbours = true
        }
        if (update_neighbours) {
            val world = getLevel()!!
            val block = getBlock()
            handleNeighborChanged(fromPos).forEach { chpos, frpos -> world.neighborChanged(chpos, block, frpos) }
        }
        return getWireFlags() != 0
    }

    private fun getNonWireSignal(world: Level, pos: BlockPos, redstone_side: Direction): Int {
        getBlock().disablePower(true)
        val state = world.getBlockState(pos)
        var p = if (!state.`is`(Blocks.REDSTONE_WIRE) && !state.`is`(getBlock())) state.getSignal(world, pos, redstone_side) else 0
        if (!RsSignals.canEmitWeakPower(state, world, pos, redstone_side)) { getBlock().disablePower(false); return p }
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

    private fun isNetConnectedTo(pos: BlockPos, net: TrackNet, otherPos: BlockPos, @Nullable otherSide: Direction?, @Nullable otherNet: TrackNet?): Boolean {
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
        nets_.filter { it.neighbour_positions.contains(fromPos) }.forEach { net ->
            handleNetNeighborChanged(net, fromPos, null, notifications)
        }
        val fst = getLevel()!!.getBlockState(fromPos)
        if (fst.`is`(getBlock()) || fst.isSignalSource) notifications.remove(fromPos)
        if (trace_ && notifications.isNotEmpty()) Auxiliaries.logWarn(String.format("NBCH: %s updates: [%s]", posstr(blockPos), notifications.entries.joinToString(", ") { "${posstr(it.value)}>${posstr(it.key)}" }))
        return notifications
    }

    fun handleNetNeighborChanged(net: TrackNet, fromPos: BlockPos, @Nullable fromNet: TrackNet?, @Nullable change_notifications: MutableMap<BlockPos, BlockPos>?) {
        data class Neighbor(val pos: BlockPos, val side: Direction, val power: Int, val direct_update: Boolean, val needs_indirect: Boolean)

        val my_pos = blockPos
        if (!isNetConnectedTo(my_pos, net, fromPos, null, fromNet)) return
        val world = getLevel()!!
        val neighbors = LinkedList<Neighbor>()
        if (trace_) Auxiliaries.logWarn(String.format("NBCH: %s from %s (%s)", posstr(my_pos), posstr(fromPos), world.getBlockState(fromPos).block.descriptionId))
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
                val nb_net = RedstoneTrackBlock.tile(world, ext_pos).flatMap { te ->
                    te.nets_.stream().filter { nbn -> isNetConnectedTo(my_pos, net, ext_pos, ext_side, nbn) }.findFirst()
                }.orElse(null)
                if (nb_net != null) {
                    val p_track = maxOf(0, nb_net.power)
                    neighbors.add(Neighbor(ext_pos, ext_side, p_track, true, false))
                    pmax = maxOf(pmax, p_track - 1)
                }
            } else if (ext_state.`is`(ModContent.references.BRIDGE_RELAY_BLOCK)) {
                val p_nowire = getNonWireSignal(world, ext_pos, ext_side.opposite)
                neighbors.add(Neighbor(ext_pos, ext_side, p_nowire, true, false))
                pmax = maxOf(pmax, p_nowire)
            } else {
                val p_nowire = getNonWireSignal(world, ext_pos, ext_side.opposite)
                val weak_updates = !ext_state.isSignalSource && p_nowire == 0 && ext_state.isRedstoneConductor(world, ext_pos)
                neighbors.add(Neighbor(ext_pos, ext_side, p_nowire, false, weak_updates))
                pmax = maxOf(pmax, p_nowire)
            }
        }
        var power_changed = false
        if (net.power != pmax) {
            if (trace_) Auxiliaries.logWarn(String.format("NBCH: %s net power %d->%d", posstr(my_pos), net.power, pmax))
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
                    world.getBlockState(neighbor.pos).handleNeighborChanged(world, neighbor.pos, getBlock(), my_pos, false)
                }
            } else {
                change_notifications?.putIfAbsent(neighbor.pos, my_pos)
                if (neighbor.needs_indirect) {
                    for (update_direction in RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS) {
                        if (neighbor.side == update_direction) continue
                        change_notifications?.putIfAbsent(neighbor.pos.relative(update_direction), neighbor.pos)
                    }
                }
            }
        }
        sync(true)
    }

    @Suppress("ALL")
    private fun isRedstoneInsulator(state: BlockState, pos: BlockPos): Boolean =
        state.`is`(Blocks.GLASS) || state.`is`(Blocks.AIR)

    internal fun updateConnections(recursion_left: Int) {
        val all_neighbours = HashSet<BlockPos>()
        val current_side_powers = IntArray(6)
        val track_connection_updates = mutableSetOf<TrackBlockEntity>()
        val internal_connected_sides = LongArray(6)
        val external_connected_routes = LongArray(6)

        run {
            nets_.forEach { net ->
                net.internal_sides.forEach { ps -> current_side_powers[ps.ordinal] = net.power }
                all_neighbours.addAll(net.neighbour_positions)
            }
            if (trace_) Auxiliaries.logWarn(String.format("UCON: %s SIDPW: [%01x %01x %01x %01x %01x %01x]", posstr(blockPos), current_side_powers[0], current_side_powers[1], current_side_powers[2], current_side_powers[3], current_side_powers[4], current_side_powers[5]))
            nets_.clear()
        }

        run {
            var external_connection_flags = getStateFlags() and (RedstoneTrackDefs.STATE_FLAG_WIR_MASK or RedstoneTrackDefs.STATE_FLAG_CON_MASK)
            for ((wire_bit_pair, _) in RedstoneTrackDefs.connections.INTERNAL_EDGE_CONNECTION_MAPPING) {
                if ((getStateFlags() and wire_bit_pair) != wire_bit_pair) continue
                external_connection_flags = external_connection_flags and wire_bit_pair.inv()
                for (i in 0 until 6) {
                    if (((0xfL shl (4 * i)) and wire_bit_pair) == 0L) continue
                    internal_connected_sides[i] = internal_connected_sides[i] or wire_bit_pair
                }
            }
            if (trace_) Auxiliaries.logWarn(String.format("UCON: %s CONFL: ext:%08x | int:[%08x %08x %08x %08x %08x %08x]", posstr(blockPos), external_connection_flags, internal_connected_sides[0], internal_connected_sides[1], internal_connected_sides[2], internal_connected_sides[3], internal_connected_sides[4], internal_connected_sides[5]))
            for (k in 0 until 2) {
                for (i in 0 until 6) {
                    if (internal_connected_sides[i] == 0L) continue
                    for (j in i + 1 until 6) {
                        if ((internal_connected_sides[i] and internal_connected_sides[j]) == 0L) continue
                        internal_connected_sides[i] = internal_connected_sides[i] or internal_connected_sides[j]
                        internal_connected_sides[j] = 0L
                    }
                }
            }
            for (i in 0 until 6) {
                if (internal_connected_sides[i] != 0L) {
                    for (j in i until 6) {
                        val mask = 0xfL shl (4 * j)
                        if ((internal_connected_sides[i] and mask) == 0L) continue
                        val bulk = 0x1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + j)
                        external_connected_routes[i] = external_connected_routes[i] or (external_connection_flags and (mask or bulk))
                        external_connection_flags = external_connection_flags and (mask or bulk).inv()
                    }
                } else {
                    val mask = 0xfL shl (4 * i)
                    val bulk = 0x1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + i)
                    external_connected_routes[i] = external_connected_routes[i] or (external_connection_flags and (mask or bulk))
                    external_connection_flags = external_connection_flags and (mask or bulk).inv()
                }
            }
            if (trace_) {
                Auxiliaries.logWarn(String.format("UCON: %s CONSD: ext:%08x | int:[%08x %08x %08x %08x %08x %08x]", posstr(blockPos), external_connection_flags, internal_connected_sides[0], internal_connected_sides[1], internal_connected_sides[2], internal_connected_sides[3], internal_connected_sides[4], internal_connected_sides[5]))
                Auxiliaries.logWarn(String.format("UCON: %s CONRT: ext:%08x | ext:[%08x %08x %08x %08x %08x %08x]", posstr(blockPos), external_connection_flags, external_connected_routes[0], external_connected_routes[1], external_connected_routes[2], external_connected_routes[3], external_connected_routes[4], external_connected_routes[5]))
            }
        }

        run {
            val used_sides = mutableSetOf<Direction>()
            for (i in 0 until 6) {
                if (external_connected_routes[i] == 0L) continue
                val int_sides = mutableSetOf<Direction>()
                val pwr_sides = ArrayList<Direction>(6)
                val positions = ArrayList<BlockPos>(6)
                val ext_sides = ArrayList<Direction>(6)
                for (j in 0 until 6) {
                    val mask = 0xfL shl (4 * j)
                    val bulk = 0x1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + j)
                    val side = connections.CONNECTION_BIT_ORDER[j]
                    if ((internal_connected_sides[i] and mask) != 0L) {
                        int_sides.add(side)
                    }
                    if ((external_connected_routes[i] and mask) != 0L) {
                        for (k in 0 until 4) {
                            val wire_bit = 0x1L shl (4 * j + k)
                            if ((external_connected_routes[i] and wire_bit) == 0L) continue
                            val _tsd = RedstoneTrackDefs.connections.getWireBitSideAndDirection(wire_bit)
                            val tsid = _tsd.getA(); val tdir = _tsd.getB()
                            val wire_pos = blockPos.relative(tdir)
                            val wire_state = getLevel()!!.getBlockState(wire_pos)
                            var diagonal_check = false
                            if (wire_state.`is`(getBlock())) {
                                val adjacent_mask = RedstoneTrackDefs.connections.getWireBit(tsid, tdir.opposite)
                                val adj_te = RedstoneTrackBlock.tile(getLevel()!!, wire_pos).orElse(null)
                                if (adj_te == null || (adj_te.getStateFlags() and adjacent_mask) != adjacent_mask) {
                                    diagonal_check = true
                                } else {
                                    positions.add(wire_pos)
                                    ext_sides.add(tsid)
                                    int_sides.add(side)
                                    pwr_sides.add(tdir)
                                    track_connection_updates.add(adj_te)
                                    continue
                                }
                            }
                            if (!diagonal_check && wire_state.`is`(Blocks.REDSTONE_WIRE)) {
                                if (side != Direction.DOWN) {
                                    diagonal_check = true
                                } else {
                                    positions.add(wire_pos)
                                    ext_sides.add(tdir.opposite)
                                    int_sides.add(side)
                                    pwr_sides.add(tdir)
                                    continue
                                }
                            }
                            if (!diagonal_check && wire_state.isSignalSource) {
                                positions.add(wire_pos)
                                ext_sides.add(tdir.opposite)
                                int_sides.add(side)
                                pwr_sides.add(tdir)
                                continue
                            }
                            run {
                                val track_pos = wire_pos.relative(tsid)
                                val track_state = getLevel()!!.getBlockState(track_pos)
                                if (track_state.`is`(getBlock())) {
                                    val adjacent_mask = RedstoneTrackDefs.connections.getWireBit(tdir.opposite, tsid.opposite)
                                    val adj_te = RedstoneTrackBlock.tile(getLevel()!!, track_pos).orElse(null)
                                    if (adj_te == null || (adj_te.getStateFlags() and adjacent_mask) != adjacent_mask) return@run
                                    positions.add(track_pos)
                                    ext_sides.add(tdir.opposite)
                                    int_sides.add(side)
                                    pwr_sides.add(tdir)
                                    track_connection_updates.add(adj_te)
                                    return@run
                                }
                            }
                            if (!isRedstoneInsulator(wire_state, wire_pos)) {
                                positions.add(wire_pos)
                                ext_sides.add(tdir.opposite)
                                int_sides.add(side)
                                pwr_sides.add(tdir)
                            }
                        }
                    }
                    if ((external_connected_routes[i] and bulk) != 0L) {
                        val bulk_pos = blockPos.relative(side)
                        val bulk_state = getLevel()!!.getBlockState(bulk_pos)
                        if (isRedstoneInsulator(bulk_state, bulk_pos)) continue
                        positions.add(bulk_pos)
                        ext_sides.add(side.opposite)
                        int_sides.add(side)
                        pwr_sides.add(side)
                    }
                }
                if (positions.isNotEmpty()) {
                    val net = TrackNet(positions, ext_sides, ArrayList(int_sides), ArrayList(pwr_sides))
                    net.power = net.internal_sides.maxOfOrNull { current_side_powers[it.ordinal] } ?: 0
                    nets_.add(net)
                    used_sides.addAll(int_sides)
                }
            }
            Direction.values().filter { !used_sides.contains(it) }.forEach { setSidePower(it, 0) }
            setChanged()
        }

        run {
            val disconnected_neighbours = HashSet<BlockPos>(all_neighbours)
            val connected_neighbours = HashSet<BlockPos>()
            nets_.forEach { net -> net.neighbour_positions.forEach { disconnected_neighbours.remove(it) } }
            nets_.forEach { net -> connected_neighbours.addAll(net.neighbour_positions) }
            all_neighbours.forEach { connected_neighbours.remove(it) }
            if (trace_) {
                val poss = posstr(blockPos)
                for (net in nets_) {
                    val ss = ArrayList<String>()
                    for (i in 0 until net.neighbour_positions.size) ss.add(posstr(net.neighbour_positions[i]) + ":" + net.neighbour_sides[i].toString())
                    val int_sides = net.internal_sides.joinToString(",") { it.toString() }
                    val pwr_sides = net.power_sides.joinToString(",") { it.toString() }
                    Auxiliaries.logWarn(String.format("UCON: %s adj:%s | ints:%s | pwrs:%s", poss, ss.joinToString(", "), int_sides, pwr_sides))
                }
                if (disconnected_neighbours.isNotEmpty()) Auxiliaries.logWarn(String.format("UCON: %s DISCONNECTED NEIGHBOURS: %s", posstr(blockPos), disconnected_neighbours.joinToString(",") { posstr(it) }))
                if (connected_neighbours.isNotEmpty()) Auxiliaries.logWarn(String.format("UCON: %s CONNECTED NEIGHBOURS: %s", posstr(blockPos), connected_neighbours.joinToString(",") { posstr(it) }))
            }
            HashSet(disconnected_neighbours).forEach { p ->
                RedstoneTrackBlock.tile(getLevel()!!, p).ifPresent { te ->
                    track_connection_updates.add(te)
                    disconnected_neighbours.remove(p)
                }
            }
            if (trace_ && disconnected_neighbours.isNotEmpty()) Auxiliaries.logWarn(String.format("UCON: %s DISCONNECTED NONTRACK: %s", posstr(blockPos), disconnected_neighbours.joinToString(",") { posstr(it) }))
        }

        run {
            if (recursion_left > 0) {
                for (te in track_connection_updates) {
                    if (trace_) Auxiliaries.logWarn(String.format("UCON: %s UPDATE NET OF %s", posstr(blockPos), posstr(te.blockPos)))
                    te.updateConnections(recursion_left - 1)
                }
            }
        }

        run {
            nets_.filter { it.power > 0 }.forEach { net -> all_neighbours.addAll(net.neighbour_positions) }
            val world = getLevel()!!
            val state = blockState
            all_neighbours.forEach { pos ->
                val st = world.getBlockState(pos)
                if (trace_) Auxiliaries.logWarn(String.format("UCON: %s UPDATE TRACK CHANGES TO %s.", posstr(blockPos), posstr(pos)))
                st.handleNeighborChanged(world, pos, state.block, blockPos, false)
                world.updateNeighborsAt(pos, st.block)
            }
        }
    }
}
