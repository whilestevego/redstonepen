package wile.redstonepen.blocks.controlbox

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
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
        private const val NUM_OF_SLOTS = 1
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

    private val player: Player = playerInventory.player
    private val inventory: Container = blockInventory
    private val wpc: ContainerLevelAccess = wpc
    private val fields: ContainerData = fields

    @Volatile private var receivedServerData: CompoundTag = CompoundTag()

    init {
        this.wpc.execute { _, _ -> this.inventory.startOpen(this.player) }
        addDataSlots(this.fields)
        for (x in 0 until 9) addSlot(Slot(playerInventory, x, 28 + x * 18, 183))
    }

    fun field(index: Int): Int = fields.get(index)

    fun player(): Player = player

    fun inventory(): Container = inventory

    fun world(): Level = player.level()

    fun te(): ControlBoxBlockEntity? =
        wpc.evaluate { w, p -> w.getBlockEntity(p) as? ControlBoxBlockEntity }.orElse(null)

    override fun stillValid(player: Player): Boolean = inventory.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        inventory.stopOpen(player)
    }

    override fun sendAllDataToRemote() {
        super.sendAllDataToRemote()
        if (world().isClientSide) return
        val te = te() ?: return
        Networking.PacketContainerSyncServerToClient.sendToListeners(
            world(),
            this,
            composeServerData(te, true),
        )
    }

    override fun quickMoveStack(player: Player, slot: Int): ItemStack = ItemStack.EMPTY

    fun composeServerData(te: ControlBoxBlockEntity, full: Boolean): CompoundTag =
        te.collectSyncData(full)

    fun fetchReceivedServerData(): CompoundTag {
        val received = receivedServerData
        receivedServerData = CompoundTag()
        return received
    }

    override fun onServerPacketReceived(windowId: Int, nbt: CompoundTag) {
        when (nbt.getString("action")) {
            "serverdata" -> {
                receivedServerData = nbt
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
