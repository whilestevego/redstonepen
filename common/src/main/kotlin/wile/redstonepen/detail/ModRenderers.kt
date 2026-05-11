package wile.redstonepen.detail

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import java.util.WeakHashMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import wile.redstonepen.ModConstants
import wile.redstonepen.blocks.track.RedstoneTrackDefs
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.platform.PlatformServices
import wile.redstonepen.util.Auxiliaries

object ModRenderers {

    @Environment(EnvType.CLIENT)
    class TrackTer(_ctx: BlockEntityRendererProvider.Context?) :
        BlockEntityRenderer<TrackBlockEntity> {

        companion object {
            private val model_rls =
                arrayOfNulls<ResourceLocation>(RedstoneTrackDefs.STATE_FLAG_WIR_COUNT)
            private val modelm_rls =
                arrayOfNulls<ResourceLocation>(RedstoneTrackDefs.STATE_FLAG_CON_COUNT)
            private val modelc_rls =
                arrayOfNulls<ResourceLocation>(RedstoneTrackDefs.STATE_FLAG_CON_COUNT)
            private val power_rgb = ArrayList<Vec3>()

            @JvmStatic
            fun registerModels(): List<ResourceLocation> {
                val resources_to_register = ArrayList<ResourceLocation>()
                RedstoneTrackDefs.models.STATE_WIRE_MAPPING.forEach { (key, value) ->
                    val mrl =
                        ResourceLocation.tryBuild(ModConstants.MODID, value)!!.withPrefix("item/")
                    for (i in 0 until RedstoneTrackDefs.STATE_FLAG_WIR_COUNT) {
                        if ((key and (1L shl (RedstoneTrackDefs.STATE_FLAG_WIR_POS + i))) != 0L) {
                            model_rls[i] = mrl
                            break
                        }
                    }
                    resources_to_register.add(mrl)
                }
                RedstoneTrackDefs.models.STATE_CONNECT_MAPPING.forEach { (key, value) ->
                    val mrl =
                        ResourceLocation.tryBuild(ModConstants.MODID, value)!!.withPrefix("item/")
                    for (i in 0 until RedstoneTrackDefs.STATE_FLAG_CON_COUNT) {
                        if ((key and (1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + i))) != 0L) {
                            modelc_rls[i] = mrl
                            break
                        }
                    }
                    resources_to_register.add(mrl)
                }
                RedstoneTrackDefs.models.STATE_CNTWIRE_MAPPING.forEach { (key, value) ->
                    val mrl =
                        ResourceLocation.tryBuild(ModConstants.MODID, value)!!.withPrefix("item/")
                    for (i in 0 until RedstoneTrackDefs.STATE_FLAG_CON_COUNT) {
                        if ((key and (1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + i))) != 0L) {
                            modelm_rls[i] = mrl
                            break
                        }
                    }
                    resources_to_register.add(mrl)
                }
                power_rgb.clear()
                for (i in 0..15) {
                    val f = i.toFloat() / 15.0f
                    power_rgb.add(
                        Vec3(
                            Mth.clamp(0.01f + f, 0.0f, 1.0f).toDouble(),
                            Mth.clamp(0.01f + f * 0.4f - 0.3f, 0.0f, 1.0f).toDouble(),
                            Mth.clamp(0.01f + f * 0.4f - 0.2f, 0.0f, 1.0f).toDouble(),
                        )
                    )
                }
                return resources_to_register
            }

            private fun getPowerRGB(p: Int): Vec3 = power_rgb[p and 0xf]
        }

        private val broken_entities_ = WeakHashMap<TrackBlockEntity, Long>()

        @Suppress("DEPRECATION")
        override fun render(
            te: TrackBlockEntity,
            unused1: Float,
            mxs: PoseStack,
            buf: MultiBufferSource,
            combinedLightIn: Int,
            combinedOverlayIn: Int,
        ) {
            val current_flags = te.getStateFlags()
            if (broken_entities_.getOrDefault(te, current_flags.inv()) == current_flags) return
            mxs.pushPose()
            try {
                val block_state: BlockState = te.blockState
                val vxb: VertexConsumer =
                    buf.getBuffer(ItemBlockRenderTypes.getRenderType(block_state, false))
                val overlay = OverlayTexture.pack(0, 0)
                run {
                    val wirfl = te.getWireFlags()
                    val wirfc = te.getWireFlagCount()
                    var flag = 0x1L
                    for (i in 0 until wirfc) {
                        if ((wirfl.toLong() and flag) != 0L) {
                            val rgb =
                                getPowerRGB(
                                    te.getSidePower(
                                        RedstoneTrackDefs.connections.CONNECTION_BIT_ORDER[i / 4]
                                    )
                                )
                            val model =
                                PlatformServices.getRendering()
                                    .getBakedModel(
                                        Minecraft.getInstance().modelManager,
                                        model_rls[i]!!,
                                    )
                            Minecraft.getInstance()
                                .blockRenderer
                                .modelRenderer
                                .renderModel(
                                    mxs.last(),
                                    vxb,
                                    null,
                                    model,
                                    rgb.x().toFloat(),
                                    rgb.y().toFloat(),
                                    rgb.z().toFloat(),
                                    combinedLightIn,
                                    overlay,
                                )
                        }
                        flag = flag shl 1
                    }
                }
                run {
                    val wirfl = te.getWireFlags()
                    val confl = te.getConnectionFlags()
                    val confc = te.getConnectionFlagCount()
                    var wir = 0xfL
                    var con = 0x1L
                    for (i in 0 until confc) {
                        if ((wirfl.toLong() and wir) != 0L || (confl.toLong() and con) != 0L) {
                            val rgb =
                                getPowerRGB(
                                    te.getSidePower(
                                        RedstoneTrackDefs.connections.CONNECTION_BIT_ORDER[i]
                                    )
                                )
                            val model =
                                if ((confl.toLong() and con) == 0L) {
                                    PlatformServices.getRendering()
                                        .getBakedModel(
                                            Minecraft.getInstance().modelManager,
                                            modelm_rls[i]!!,
                                        )
                                } else {
                                    PlatformServices.getRendering()
                                        .getBakedModel(
                                            Minecraft.getInstance().modelManager,
                                            modelc_rls[i]!!,
                                        )
                                }
                            Minecraft.getInstance()
                                .blockRenderer
                                .modelRenderer
                                .renderModel(
                                    mxs.last(),
                                    vxb,
                                    null,
                                    model,
                                    rgb.x().toFloat(),
                                    rgb.y().toFloat(),
                                    rgb.z().toFloat(),
                                    combinedLightIn,
                                    overlay,
                                )
                        }
                        con = con shl 1
                        wir = wir shl 4
                    }
                }
            } catch (e: Throwable) {
                if (!broken_entities_.containsKey(te)) {
                    Auxiliaries.logError(
                        "TER render error for track at ${te.blockPos}, exception: ${e.message}"
                    )
                    Auxiliaries.logError(e.stackTrace.joinToString("\n") { it.toString() })
                }
                broken_entities_[te] = current_flags
            } finally {
                mxs.popPose()
            }
        }
    }
}
