package wile.redstonepen.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import wile.redstonepen.ModConstants
import wile.redstonepen.ModContent
import wile.redstonepen.commands.DemoCommand
import wile.redstonepen.detail.RcaSync
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries

class ModRedstonePen : ModInitializer {
    override fun onInitialize() {
        Auxiliaries.init()
        Auxiliaries.logGitVersion()
        Registries.init()
        ModContent.init()
        Registries.instantiateAll()
        ModContent.initReferences()
        Networking.init()
        RcaSync.CommonRca.init()
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "creative_tab"), CREATIVE_TAB)
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            DemoCommand.register(dispatcher)
        }
    }

    companion object {
        private val CREATIVE_TAB: CreativeModeTab = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.tab${ModConstants.MODID}"))
            .icon { ItemStack(Registries.getItem("pen")) }
            .displayItems { _, reg ->
                Registries.getRegisteredItems().forEach {
                    if (it !is BlockItem || it.block != ModContent.References.TRACK_BLOCK) reg.accept(it)
                }
            }
            .build()
    }
}
