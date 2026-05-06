package wile.redstonepen.libmc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import java.util.List;

@Environment(EnvType.CLIENT)
public interface IRenderingPlatform
{
  void setRenderLayer(Block block, RenderType renderType);
  void registerAdditionalModels(List<ResourceLocation> models);
  BakedModel getBakedModel(ModelManager modelManager, ResourceLocation location);
}
