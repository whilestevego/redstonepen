package wile.redstonepen.blocks.controlbox

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries

@Suppress("DEPRECATION")
class ControlBoxUiContainer(
    cid: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    wpc: ContainerLevelAccess,
    fields: ContainerData,
) :
    AbstractContainerMenu(Registries.getMenuTypeOfBlock("control_box"), cid),
    Networking.INetworkSynchronisableContainer {

    companion object {
        protected const val NUM_OF_SLOTS = 1
    }

    constructor(
        cid: Int,
        playerInventory: Inventory,
    ) : this(
        cid,
        playerInventory,
        SimpleContainer(NUM_OF_SLOTS),
        ContainerLevelAccess.NULL,
        SimpleContainerData(1),
    )

    private val player_: Player = playerInventory.player
    private val inventory_: Container = blockInventory
    private val wpc_: ContainerLevelAccess = wpc
    private val fields_: ContainerData = fields

    @Volatile private var received_server_data_: CompoundTag = CompoundTag()

    init {
        wpc_.execute { _, _ -> inventory_.startOpen(player_) }
        addDataSlots(fields_)
        for (x in 0 until 9) addSlot(Slot(playerInventory, x, 28 + x * 18, 183))
    }

    fun field(index: Int): Int = fields_.get(index)

    fun player(): Player = player_

    fun inventory(): Container = inventory_

    fun world(): Level = player_.level()

    fun te(): ControlBoxBlockEntity? =
        wpc_.evaluate { w, p -> w.getBlockEntity(p) as? ControlBoxBlockEntity }.orElse(null)

    override fun stillValid(player: Player): Boolean = inventory_.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        inventory_.stopOpen(player)
    }

    override fun sendAllDataToRemote() {
        super.sendAllDataToRemote()
        if (world().isClientSide || te() == null) return
        Networking.PacketContainerSyncServerToClient.sendToListeners(
            world(),
            this,
            composeServerData(te()!!, true),
        )
    }

    override fun quickMoveStack(player: Player, slot: Int): ItemStack = ItemStack.EMPTY

    fun composeServerData(te: ControlBoxBlockEntity, full: Boolean): CompoundTag =
        te.collectSyncData(full)

    fun fetchReceivedServerData(): CompoundTag {
        val received = received_server_data_
        received_server_data_ = CompoundTag()
        return received
    }

    override fun onServerPacketReceived(windowId: Int, nbt: CompoundTag) {
        when (nbt.getString("action")) {
            "serverdata" -> {
                received_server_data_ = nbt
            }
        }
    }

    override fun onClientPacketReceived(windowId: Int, player: Player, nbt: CompoundTag) {
        val te = te() ?: return
        var sync = 0
        when (nbt.getString("action")) {
            "codeupdate" -> {
                te.setCode(nbt.getString("code"))
            }
            "serverdata" -> {
                sync = 2
            }
            "servervalues" -> {
                sync = 1
            }
            "enabled" -> {
                te.setEnabled(!te.getEnabled())
                te.setRcaPlayerUUID(
                    if (te.getEnabled() && nbt.getBoolean("withrca")) player.uuid else null
                )
                sync = 2
            }
        }
        if (sync > 0) {
            Networking.PacketContainerSyncServerToClient.sendToListeners(
                world(),
                this,
                composeServerData(te, sync > 1),
            )
        }
    }
}
