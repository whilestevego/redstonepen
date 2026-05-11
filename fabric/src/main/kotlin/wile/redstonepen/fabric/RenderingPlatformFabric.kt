package wile.redstonepen.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import wile.redstonepen.platform.IRenderingPlatform

@Environment(EnvType.CLIENT)
class RenderingPlatformFabric : IRenderingPlatform {
    override fun setRenderLayer(block: Block, renderType: RenderType) {
        BlockRenderLayerMap.INSTANCE.putBlock(block, renderType)
    }

    override fun registerAdditionalModels(models: List<ResourceLocation>) {
        /* Fabric registers models via ModelLoadingPlugin in ModRedstonePenClient */
    }

    override fun getBakedModel(modelManager: ModelManager, location: ResourceLocation): BakedModel =
        modelManager.getModel(location)
}
