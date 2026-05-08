package wile.redstonepen.platform

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block

@Environment(EnvType.CLIENT)
interface IRenderingPlatform {
    fun setRenderLayer(block: Block, renderType: RenderType)
    fun registerAdditionalModels(models: List<ResourceLocation>)
    fun getBakedModel(modelManager: ModelManager, location: ResourceLocation): BakedModel
}
