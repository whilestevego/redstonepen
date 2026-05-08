package wile.redstonepen.neoforge;

import wile.redstonepen.platform.IRenderingPlatform;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RenderingPlatformNeoForge implements IRenderingPlatform
{
  @Override
  public void setRenderLayer(Block block, RenderType renderType)
  { /* NeoForge uses block's built-in render type; no external registration needed */ }

  @Override
  public void registerAdditionalModels(List<ResourceLocation> models)
  { /* Models registered via ModelEvent.RegisterAdditional in ModRedstonePen.ClientEvents */ }

  @Override
  public BakedModel getBakedModel(ModelManager modelManager, ResourceLocation location)
  { return modelManager.getModel(new ModelResourceLocation(location, "standalone")); }
}
