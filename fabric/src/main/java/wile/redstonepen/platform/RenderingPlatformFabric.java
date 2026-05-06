package wile.redstonepen.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import wile.redstonepen.libmc.IRenderingPlatform;

import java.util.List;

@Environment(EnvType.CLIENT)
public class RenderingPlatformFabric implements IRenderingPlatform
{
  @Override
  public void setRenderLayer(Block block, RenderType renderType)
  { BlockRenderLayerMap.INSTANCE.putBlock(block, renderType); }

  @Override
  public void registerAdditionalModels(List<ResourceLocation> models)
  { /* Fabric registers models via ModelLoadingPlugin in ModRedstonePenClient */ }

  @Override
  public BakedModel getBakedModel(ModelManager modelManager, ResourceLocation location)
  { return modelManager.getModel(location); }
}
