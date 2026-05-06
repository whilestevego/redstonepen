package wile.redstonepen.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import wile.redstonepen.ModConstants;
import wile.redstonepen.ModContent;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Networking;
import wile.redstonepen.libmc.Registries;

public class ModRedstonePen implements ModInitializer
{
  private static final CreativeModeTab CREATIVE_TAB = FabricItemGroup.builder()
    .title(Component.translatable("itemGroup.tab" + ModConstants.MODID))
    .icon(() -> new ItemStack(Registries.getItem("pen")))
    .displayItems((ctx, reg) -> Registries.getRegisteredItems().forEach(it -> {
      if(!(it instanceof BlockItem bit) || (bit.getBlock() != ModContent.references.TRACK_BLOCK)) reg.accept(it);
    }))
    .build();

  @Override
  public void onInitialize()
  {
    Auxiliaries.init();
    Auxiliaries.logGitVersion();
    Registries.init();
    ModContent.init();
    Registries.instantiateAll();
    ModContent.initReferences();
    Networking.init();
    wile.redstonepen.detail.RcaSync.CommonRca.init();
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
      ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "creative_tab"), CREATIVE_TAB);
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
      wile.redstonepen.commands.DemoCommand.register(dispatcher));
  }
}
