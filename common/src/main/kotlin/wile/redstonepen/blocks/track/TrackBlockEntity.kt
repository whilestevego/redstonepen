package wile.redstonepen.blocks.track

import java.util.LinkedList
import java.util.Locale
import kotlin.math.abs
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
import wile.redstonepen.blocks.track.RedstoneTrackDefs.Connections
import wile.redstonepen.items.RedstonePenItem
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries
import wile.redstonepen.util.RsSignals

@Suppress("DEPRECATION")
class TrackBlockEntity(pos: BlockPos, state: BlockState) :
    StandardEntityBlocks.StandardBlockEntity(
        requireNotNull(Registries.getBlockEntityTypeOfBlock(state.block)),
        pos,
        state,
    ),
    Networking.IPacketTileNotifyReceiver {
    private var stateFlags: TrackStateFlags = TrackStateFlags.EMPTY
    private val nets: MutableList<TrackNet> = ArrayList()
    private val blockChangeTracking: Array<Block> = Array(6) { Blocks.AIR }
    private var trace: Boolean = false

    companion object {
        @JvmStatic internal fun posstr(pos: BlockPos): String = "[${pos.x},${pos.y},${pos.z}]"

        @JvmStatic
        internal fun dirstr(@Nullable dir: Direction?): String =
            dir?.toString()?.substring(0, 1) ?: "?"

        private val updatepower_order: List<Vec3i> by lazy {
            val list = ArrayList<Vec3i>()
            for (side in Direction.entries) {
                list.add(Vec3i(0, 0, 0).relative(side, 1))
            }
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        if (abs(x) + abs(y) + abs(z) == 2) list.add(Vec3i(x, y, z))
                    }
                }
            }
            list
        }
    }

    private val requireLevel: Level
        get() = checkNotNull(level) { "level not set on $this" }

    override fun readnbt(hlp: HolderLookup.Provider, nbt: CompoundTag): CompoundTag {
        stateFlags = TrackStateFlags(nbt.getLong("sflags"))
        nets.clear()
        if (nbt.contains("nets", Tag.TAG_LIST.toInt())) {
            val lst = nbt.getList("nets", Tag.TAG_COMPOUND.toInt())
            try {
                for (i in lst.indices) {
                    val routeNbt = lst.getCompound(i)
                    nets.add(
                        TrackNet(
                            routeNbt.getLongArray("npos").map { BlockPos.of(it) },
                            routeNbt.getIntArray("nsid").map { Direction.from3DDataValue(it) },
                            routeNbt.getIntArray("ifac").map { Direction.from3DDataValue(it) },
                            routeNbt.getIntArray("pfac").map { Direction.from3DDataValue(it) },
                            routeNbt.getInt("power"),
                        )
                    )
                }
            } catch (ex: Throwable) {
                nets.clear()
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
        nbt.putLong("sflags", stateFlags.raw)
        if (syncPacket) return nbt
        if (nets.isNotEmpty()) {
            val lst = ListTag()
            for (net in nets) {
                val routeNbt = CompoundTag()
                routeNbt.putInt("power", net.power)
                routeNbt.put(
                    "npos",
                    LongArrayTag(net.neighbourPositions.map { it.asLong() }.toLongArray()),
                )
                routeNbt.put(
                    "nsid",
                    IntArrayTag(net.neighbourSides.map { it.get3DDataValue() }.toIntArray()),
                )
                routeNbt.put(
                    "ifac",
                    IntArrayTag(net.internalSides.map { it.get3DDataValue() }.toIntArray()),
                )
                routeNbt.put(
                    "pfac",
                    IntArrayTag(net.powerSides.map { it.get3DDataValue() }.toIntArray()),
                )
                lst.add(routeNbt)
            }
            nbt.put("nets", lst)
        }
        return nbt
    }

    override fun onServerPacketReceived(nbt: CompoundTag) {
        readnbt(requireLevel.registryAccess(), nbt)
    }

    override fun onClientPacketReceived(player: Player, nbt: CompoundTag) {}

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    @Suppress("FunctionOnlyReturningConstant")
    @Environment(EnvType.CLIENT)
    fun getViewDistance(): Double = 64.0

    fun sync(schedule: Boolean): Boolean {
        if (requireLevel.isClientSide()) return true
        setChanged()
        if (
            schedule &&
                !requireLevel
                    .getBlockTicks()
                    .hasScheduledTick(blockPos, ModContent.References.TRACK_BLOCK)
        ) {
            requireLevel.scheduleTick(blockPos, ModContent.References.TRACK_BLOCK, 1)
        } else {
            Networking.PacketTileNotifyServerToClient.sendToPlayers(
                this,
                writenbt(requireLevel.registryAccess(), CompoundTag(), true),
            )
        }
        return true
    }

    fun getStateFlags(): Long = stateFlags.raw

    fun addWireFlags(flags: Long): Int {
        val (newFlags, added) = stateFlags.withAddedWireFlags(flags)
        stateFlags = newFlags
        return added
    }

    fun getWireFlags(): Int = stateFlags.wireFlags

    fun getWireFlag(index: Int): Boolean = stateFlags.wireFlag(index)

    fun getWireFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_WIR_COUNT

    fun getConnectionFlags(): Int = stateFlags.connectionFlags

    fun getConnectionFlag(index: Int): Boolean = stateFlags.connectionFlag(index)

    fun getConnectionFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_CON_COUNT

    fun getSidePower(side: Direction): Int = stateFlags.sidePower(side)

    fun setSidePower(side: Direction, p: Int) {
        stateFlags = stateFlags.withSidePower(side, p)
    }

    fun hasVanillaRedstoneConnection(side: Direction): Boolean =
        RedstoneTrackDefs.Connections.hasVanillaWireConnection(getStateFlags(), side) ||
            stateFlags.hasBits(RedstoneTrackDefs.Connections.getBulkConnectorBit(side))

    fun getRedstonePower(redstoneSide: Direction, weak: Boolean): Int {
        if (isRemoved) return 0
        val ownSide = redstoneSide.opposite
        var p = 0
        for (net in nets) {
            if (!net.powerSides.contains(ownSide)) continue
            p = maxOf(p, net.power)
            if (p >= 15) break
        }
        p =
            if (
                p <= 0 ||
                    !requireLevel
                        .getBlockState(blockPos.relative(ownSide))
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
            trace = false
            if (player != null) {
                Auxiliaries.playerChatMessage(player, "Trace disabled, not in development mode.")
            }
        } else {
            trace = !trace
            if (player != null) Auxiliaries.playerChatMessage(player, "Trace: $trace")
        }
    }

    internal fun applyPenEdit(
        pos: BlockPos,
        player: Player,
        usedStack: ItemStack,
        clickedFace: Direction,
        hitvec: Vec3,
        noAdd: Boolean,
        noRemove: Boolean,
        noBulk: Boolean,
    ): Int {
        if (
            !usedStack.isEmpty &&
                usedStack.item != Items.REDSTONE &&
                !RedstonePenItem.isPen(usedStack)
        ) {
            return 0
        }
        val face = clickedFace.opposite
        val flipMask = computeFlipMask(clickedFace, face, hitvec, noAdd, noBulk)
        val materialUse = applyWireMutation(flipMask, face, noAdd, noRemove, player, usedStack)
        if (materialUse != 0) {
            propagateEdit(face, pos, getSidePower(face))
            sync(true)
        }
        return materialUse
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
            (getWireFlags() and RedstoneTrackDefs.Connections.getAllElementsOnFace(face).toInt()) ==
                0
        val shouldBulkConnect =
            !noBulk && !faceIsEmpty && hit.length() < 0.10 && (!noAdd || getConnectionFlags() != 0)
        return if (shouldBulkConnect) {
            RedstoneTrackDefs.Connections.getBulkConnectorBit(face)
        } else if (noAdd || hit.length() > 0.11) {
            var m = RedstoneTrackDefs.Connections.getWireBit(face, dir)
            if (!noAdd) {
                if (isAdjacentWireSegmentAddable(face, dir.opposite)) {
                    m = m or RedstoneTrackDefs.Connections.getWireBit(face, dir.opposite)
                }
                if (isAdjacentWireSegmentAddable(face, dir.getClockWise(face.axis))) {
                    m =
                        m or
                            RedstoneTrackDefs.Connections.getWireBit(
                                face,
                                dir.getClockWise(face.axis),
                            )
                }
                if (isAdjacentWireSegmentAddable(face, dir.getCounterClockWise(face.axis))) {
                    m =
                        m or
                            RedstoneTrackDefs.Connections.getWireBit(
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
        if (stateFlags.hasBits(flipMask)) {
            if (!noRemove) {
                stateFlags = stateFlags.withBitsCleared(flipMask)
                materialUse -= 1
                val bc = RedstoneTrackDefs.Connections.getBulkConnectorBit(face)
                if (
                    (stateFlags.raw and RedstoneTrackDefs.Connections.getAllElementsOnFace(face)) ==
                        bc
                ) {
                    stateFlags = stateFlags.withBitsCleared(bc)
                    materialUse -= 1
                }
                if (getWireFlags() == 0) {
                    materialUse -= getRedstoneDustCount()
                    stateFlags = TrackStateFlags.EMPTY
                }
            }
        } else if (!noAdd) {
            for (i in 0 until RedstoneTrackDefs.STATE_FLAG_PWR_POS) {
                val mask = 1L shl i
                if ((flipMask and mask) == 0L || (getStateFlags() and mask) != 0L) continue
                if (!RedstonePenItem.hasEnoughRedstone(usedStack, materialUse + 1, player)) break
                stateFlags = stateFlags.withBitsSet(mask)
                materialUse += 1
            }
        }
        return materialUse
    }

    private fun propagateEdit(face: Direction, originalPos: BlockPos, initialSidePower: Int) {
        setSidePower(face, 0)
        val changesBefore = updateAllPowerValuesFromAdjacent()
        val netNeighboursBefore: Set<BlockPos> =
            nets
                .firstOrNull { it.internalSides.contains(face) }
                ?.neighbourPositions
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
                RedstoneTrackDefs.Connections.hasBulkConnection(getStateFlags(), face)
        ) {
            val netNeighboursAfter: Set<BlockPos> =
                nets
                    .firstOrNull { it.internalSides.contains(face) }
                    ?.neighbourPositions
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
            nets.forEach { net -> if (net.internalSides.contains(face)) net.power = 0 }
            disconnected.forEach { p ->
                val te = requireLevel.getBlockEntity(p)
                requireLevel
                    .getBlockState(p)
                    .handleNeighborChanged(requireLevel, p, getBlock(), originalPos, false)
                if (te is TrackBlockEntity) te.updateConnections(1)
            }
            connected.forEach { p ->
                val te = requireLevel.getBlockEntity(p)
                if (te is TrackBlockEntity) te.updateConnections(1)
                requireLevel
                    .getBlockState(p)
                    .handleNeighborChanged(requireLevel, p, getBlock(), originalPos, false)
                getBlock().neighborChanged(blockState, requireLevel, blockPos, getBlock(), p, false)
            }
        }
    }

    private fun isAdjacentWireSegmentAddable(face: Direction, dir: Direction): Boolean {
        if ((getStateFlags() and RedstoneTrackDefs.Connections.getWireBit(face, dir)) != 0L) {
            return false
        }
        val pos = blockPos.relative(dir)
        val te = RedstoneTrackBlock.tile(requireLevel, pos).orElse(null)
        if (te != null) {
            return (te.getStateFlags() and
                RedstoneTrackDefs.Connections.getWireBit(face, dir.opposite)) != 0L
        }
        val state = requireLevel.getBlockState(pos)
        return state.`is`(Blocks.REDSTONE_WIRE) || state.isSignalSource
    }

    fun updateAllPowerValuesFromAdjacent(): Map<BlockPos, BlockPos> {
        val allChangeNotifications = HashMap<BlockPos, BlockPos>()
        for (ofs in updatepower_order) {
            handleNeighborChanged(blockPos.offset(ofs)).forEach(allChangeNotifications::putIfAbsent)
        }
        return allChangeNotifications
    }

    private fun spawnRedstoneItems(count: Int) {
        if (count <= 0) return
        val e =
            ItemEntity(
                requireLevel,
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5,
                ItemStack(Items.REDSTONE, count),
            )
        e.setDefaultPickUpDelay()
        e.setDeltaMovement(
            Vec3(
                    requireLevel.random.nextDouble() - .5,
                    requireLevel.random.nextDouble() - .5,
                    requireLevel.random.nextDouble(),
                )
                .scale(0.1)
        )
        requireLevel.addFreshEntity(e)
    }

    private fun getBlock(): RedstoneTrackBlock = ModContent.References.TRACK_BLOCK

    fun handleShapeUpdate(
        facing: Direction,
        facingState: BlockState,
        fromPos: BlockPos,
        isMoving: Boolean,
    ): Boolean {
        var updateNeighbours = false
        if (
            !RedstoneTrackBlock.canBePlacedOnFace(
                facingState,
                requireLevel,
                fromPos,
                facing.opposite,
            )
        ) {
            val toRemove = RedstoneTrackDefs.Connections.getAllElementsOnFace(facing)
            val newFlags = stateFlags.withBitsCleared(toRemove)
            if (newFlags != stateFlags) {
                if (trace) {
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
                stateFlags = newFlags
                count -= getRedstoneDustCount()
                spawnRedstoneItems(count)
                updateConnections(1)
                updateNeighbours = true
            }
        }
        val bltv: Block = blockChangeTracking[facing.get3DDataValue()]
        if (bltv != facingState.block) {
            if (trace) {
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
            blockChangeTracking[facing.get3DDataValue()] = facingState.block
            if (!isMoving && bltv != Blocks.REDSTONE_BLOCK) updateConnections(1)
            updateNeighbours = true
        }
        if (updateNeighbours) {
            val world = requireLevel
            val block = getBlock()
            handleNeighborChanged(fromPos).forEach { chpos, frpos ->
                world.neighborChanged(chpos, block, frpos)
            }
        }
        return getWireFlags() != 0
    }

    private fun getNonWireSignal(world: Level, pos: BlockPos, rsSide: Direction): Int {
        getBlock().disablePower(true)
        val state = world.getBlockState(pos)
        var p =
            if (!state.`is`(Blocks.REDSTONE_WIRE) && !state.`is`(getBlock())) {
                state.getSignal(world, pos, rsSide)
            } else {
                0
            }
        if (!RsSignals.canEmitWeakPower(state, world, pos, rsSide)) {
            getBlock().disablePower(false)
            return p
        }
        for (nbSide in Direction.entries) {
            val sidePos = pos.relative(nbSide)
            val sideState = world.getBlockState(sidePos)
            if (sideState.`is`(Blocks.REDSTONE_WIRE) || sideState.`is`(getBlock())) continue
            val pIn = sideState.getDirectSignal(world, sidePos, nbSide)
            if (pIn > p) {
                p = pIn
                if (p >= 15) break
            }
        }
        getBlock().disablePower(false)
        return p
    }

    private fun isNetConnectedTo(
        pos: BlockPos,
        net: TrackNet,
        otherPos: BlockPos,
        @Nullable otherSide: Direction?,
        @Nullable otherNet: TrackNet?,
    ): Boolean {
        if (otherNet == null) return net.neighbourPositions.any { it == otherPos }
        for (i in 0 until net.neighbourPositions.size) {
            if (net.neighbourPositions[i] != otherPos) continue
            val nbSide = net.neighbourSides[i]
            if (otherSide != null && otherSide != nbSide) continue
            if (!otherNet.internalSides.contains(nbSide)) continue
            return true
        }
        return false
    }

    fun handleNeighborChanged(fromPos: BlockPos): Map<BlockPos, BlockPos> {
        val notifications = LinkedHashMap<BlockPos, BlockPos>()
        nets
            .filter { it.neighbourPositions.contains(fromPos) }
            .forEach { net -> handleNetNeighborChanged(net, fromPos, null, notifications) }
        val fst = requireLevel.getBlockState(fromPos)
        if (fst.`is`(getBlock()) || fst.isSignalSource) notifications.remove(fromPos)
        if (trace && notifications.isNotEmpty()) {
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
        @Nullable changeNotifications: MutableMap<BlockPos, BlockPos>?,
    ) {
        data class Neighbor(
            val pos: BlockPos,
            val side: Direction,
            val power: Int,
            val directUpdate: Boolean,
            val needsIndirect: Boolean,
        )

        val myPos = blockPos
        if (!isNetConnectedTo(myPos, net, fromPos, null, fromNet)) return
        val world = requireLevel
        val neighbors = LinkedList<Neighbor>()
        if (trace) {
            Auxiliaries.logWarn(
                String.format(
                    Locale.ROOT,
                    "NBCH: %s from %s (%s)",
                    posstr(myPos),
                    posstr(fromPos),
                    world.getBlockState(fromPos).block.descriptionId,
                )
            )
        }
        var pmax = 0
        for (i in 0 until net.neighbourPositions.size) {
            val extPos = net.neighbourPositions[i]
            val extSide = net.neighbourSides[i]
            val extState = requireLevel.getBlockState(extPos)
            if (extState.`is`(Blocks.REDSTONE_WIRE)) {
                val pVanillaWire = extState.getValue(RedStoneWireBlock.POWER)
                neighbors.add(
                    Neighbor(
                        extPos,
                        extSide,
                        pVanillaWire,
                        directUpdate = false,
                        needsIndirect = false,
                    )
                )
                pmax = maxOf(pmax, pVanillaWire - 1)
            } else if (extState.`is`(getBlock())) {
                val nbNet =
                    RedstoneTrackBlock.tile(world, extPos)
                        .flatMap { te ->
                            te.nets
                                .stream()
                                .filter { nbn ->
                                    isNetConnectedTo(myPos, net, extPos, extSide, nbn)
                                }
                                .findFirst()
                        }
                        .orElse(null)
                if (nbNet != null) {
                    val pTrack = maxOf(0, nbNet.power)
                    neighbors.add(
                        Neighbor(
                            extPos,
                            extSide,
                            pTrack,
                            directUpdate = true,
                            needsIndirect = false,
                        )
                    )
                    pmax = maxOf(pmax, pTrack - 1)
                }
            } else if (extState.`is`(ModContent.References.BRIDGE_RELAY_BLOCK)) {
                val pNowire = getNonWireSignal(world, extPos, extSide.opposite)
                neighbors.add(
                    Neighbor(extPos, extSide, pNowire, directUpdate = true, needsIndirect = false)
                )
                pmax = maxOf(pmax, pNowire)
            } else {
                val pNowire = getNonWireSignal(world, extPos, extSide.opposite)
                val weakUpdates =
                    !extState.isSignalSource &&
                        pNowire == 0 &&
                        extState.isRedstoneConductor(world, extPos)
                neighbors.add(Neighbor(extPos, extSide, pNowire, false, weakUpdates))
                pmax = maxOf(pmax, pNowire)
            }
        }
        var powerChanged = false
        if (net.power != pmax) {
            if (trace) {
                Auxiliaries.logWarn(
                    String.format(
                        Locale.ROOT,
                        "NBCH: %s net power %d->%d",
                        posstr(myPos),
                        net.power,
                        pmax,
                    )
                )
            }
            net.power = pmax
            powerChanged = true
        }
        for (side in net.internalSides) {
            if (getSidePower(side) != pmax) {
                setSidePower(side, pmax)
                powerChanged = true
            }
        }
        if (!powerChanged) return
        for (neighbor in neighbors) {
            if (neighbor.directUpdate) {
                val be = world.getBlockEntity(neighbor.pos)
                if (be is TrackBlockEntity) {
                    for (nbNet in be.nets) {
                        be.handleNetNeighborChanged(nbNet, myPos, net, changeNotifications)
                    }
                } else {
                    world
                        .getBlockState(neighbor.pos)
                        .handleNeighborChanged(world, neighbor.pos, getBlock(), myPos, false)
                }
            } else {
                changeNotifications?.putIfAbsent(neighbor.pos, myPos)
                if (neighbor.needsIndirect) {
                    for (updateDirection in RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS) {
                        if (neighbor.side == updateDirection) continue
                        changeNotifications?.putIfAbsent(
                            neighbor.pos.relative(updateDirection),
                            neighbor.pos,
                        )
                    }
                }
            }
        }
        sync(true)
    }

    internal fun updateConnections(recursionLeft: Int) {
        val result =
            TrackNetworkCalculator(requireLevel, blockPos, stateFlags, getBlock(), trace)
                .calculate(nets)
        stateFlags = result.newStateFlags
        nets.clear()
        nets.addAll(result.nets)
        setChanged()
        if (recursionLeft > 0) {
            for (te in result.trackConnectionUpdates) {
                if (trace) {
                    Auxiliaries.logWarn(
                        String.format(
                            Locale.ROOT,
                            "UCON: %s UPDATE NET OF %s",
                            posstr(blockPos),
                            posstr(te.blockPos),
                        )
                    )
                }
                te.updateConnections(recursionLeft - 1)
            }
        }
        val world = requireLevel
        val state = blockState
        result.neighboursToNotify.forEach { pos ->
            val st = world.getBlockState(pos)
            if (trace) {
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
