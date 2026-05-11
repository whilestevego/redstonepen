package wile.redstonepen.neoforge

import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import wile.redstonepen.platform.IRenderingPlatform

@OnlyIn(Dist.CLIENT)
class RenderingPlatformNeoForge : IRenderingPlatform {
    override fun setRenderLayer(block: Block, renderType: RenderType) {}

    override fun registerAdditionalModels(models: List<ResourceLocation>) {}

    override fun getBakedModel(modelManager: ModelManager, location: ResourceLocation): BakedModel =
        modelManager.getModel(ModelResourceLocation(location, "standalone"))
}
