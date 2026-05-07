/*
 * @file ModRenderers.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen.detail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.track.RedstoneTrackDefs;
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections;
import wile.redstonepen.blocks.track.TrackBlockEntity;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.PlatformServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;


public class ModRenderers
{
  @Environment(EnvType.CLIENT)
  public static class TrackTer implements BlockEntityRenderer<TrackBlockEntity>
  {
    private static final ResourceLocation[] model_rls  = new ResourceLocation[RedstoneTrackDefs.STATE_FLAG_WIR_COUNT];
    private static final ResourceLocation[] modelm_rls = new ResourceLocation[RedstoneTrackDefs.STATE_FLAG_CON_COUNT];
    private static final ResourceLocation[] modelc_rls = new ResourceLocation[RedstoneTrackDefs.STATE_FLAG_CON_COUNT];
    private static final ArrayList<Vec3> power_rgb = new ArrayList<>();
    private final WeakHashMap<TrackBlockEntity, Long> broken_entities_ = new WeakHashMap<>();
    private final BlockEntityRendererProvider.Context renderer_;

    public static List<ResourceLocation> registerModels()
    {
      List<ResourceLocation> resources_to_register = new ArrayList<>();

      RedstoneTrackDefs.models.STATE_WIRE_MAPPING.entrySet().forEach((kv->{
        final ResourceLocation mrl = ResourceLocation.tryBuild(ModConstants.MODID, kv.getValue()).withPrefix("item/");
        for(int i=0; i<RedstoneTrackDefs.STATE_FLAG_WIR_COUNT; ++i) {
          if((kv.getKey() & (1L<<(RedstoneTrackDefs.STATE_FLAG_WIR_POS+i))) != 0) {
            model_rls[i] = mrl;
            break;
          }
        }
        resources_to_register.add(mrl);
      }));
      RedstoneTrackDefs.models.STATE_CONNECT_MAPPING.entrySet().forEach((kv->{
        ResourceLocation mrl = ResourceLocation.tryBuild(ModConstants.MODID, kv.getValue()).withPrefix("item/");
        for(int i=0; i<RedstoneTrackDefs.STATE_FLAG_CON_COUNT; ++i) {
          if((kv.getKey() & (1L<<(RedstoneTrackDefs.STATE_FLAG_CON_POS+i))) != 0) {
            modelc_rls[i] = mrl;
            break;
          }
        }
        resources_to_register.add(mrl);
      }));
      RedstoneTrackDefs.models.STATE_CNTWIRE_MAPPING.entrySet().forEach((kv->{
        ResourceLocation mrl = ResourceLocation.tryBuild(ModConstants.MODID, kv.getValue()).withPrefix("item/");
        for(int i=0; i<RedstoneTrackDefs.STATE_FLAG_CON_COUNT; ++i) {
          if((kv.getKey() & (1L<<(RedstoneTrackDefs.STATE_FLAG_CON_POS+i))) != 0) {
            modelm_rls[i] = mrl;
            break;
          }
        }
        resources_to_register.add(mrl);
      }));
      power_rgb.clear();
      for(int i = 0; i <= 15; ++i) {
        float f = (float)i / 15.0f;
        power_rgb.add(new Vec3(
          Mth.clamp(0.01f + f, 0.0F, 1f),
          Mth.clamp(0.01f + f * 0.4f-.3f, 0.0F, 1f),
          Mth.clamp(0.01f + f * 0.4f-.2f, 0.0F, 1f)
        ));
      }
      return resources_to_register;
    }

    private static Vec3 getPowerRGB(int p)
    { return power_rgb.get(p & 0xf); }

    public TrackTer(BlockEntityRendererProvider.Context renderer)
    { this.renderer_ = renderer; }

    @Override
    @SuppressWarnings("deprecation")
    public void render(final TrackBlockEntity te, float unused1, PoseStack mxs, MultiBufferSource buf, int combinedLightIn, int combinedOverlayIn)
    {
      final long current_flags = te.getStateFlags();
      if(broken_entities_.getOrDefault(te, ~current_flags) == current_flags) return;
      mxs.pushPose();
      try {
        final BlockState block_state = te.getBlockState();
        final VertexConsumer vxb = buf.getBuffer(ItemBlockRenderTypes.getRenderType(block_state, false));
        combinedOverlayIn = OverlayTexture.pack(0, 0);
        {
          final int wirfl = te.getWireFlags();
          final int wirfc = te.getWireFlagCount();
          long flag = 0x1;
          for(int i=0; i<wirfc; ++i, flag<<=1) {
            if((wirfl & flag) == 0) continue;
            final Vec3 rgb = getPowerRGB(te.getSidePower(connections.CONNECTION_BIT_ORDER[i/4]));
            final BakedModel model = PlatformServices.getRendering().getBakedModel(Minecraft.getInstance().getModelManager(), model_rls[i]);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
              mxs.last(),
              vxb,
              null,
              model,
              (float)rgb.x(), (float)rgb.y(), (float)rgb.z(),
              combinedLightIn,
              combinedOverlayIn
            );
          }
        }
        {
          final int wirfl = te.getWireFlags();
          final int confl = te.getConnectionFlags();
          final int confc = te.getConnectionFlagCount();
          long wir = 0xfL;
          long con = 0x1L;
          for(int i=0; i<confc; ++i, con<<=1, wir<<=4) {
            if(((wirfl & wir)==0) && ((confl & con)==0)) continue;
            final Vec3 rgb = getPowerRGB(te.getSidePower(connections.CONNECTION_BIT_ORDER[i]));
            final BakedModel model = ((confl & con)==0)
              ? PlatformServices.getRendering().getBakedModel(Minecraft.getInstance().getModelManager(), modelm_rls[i])
              : PlatformServices.getRendering().getBakedModel(Minecraft.getInstance().getModelManager(), modelc_rls[i]);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
              mxs.last(),
              vxb,
              null,
              model,
              (float)rgb.x(), (float)rgb.y(), (float)rgb.z(),
              combinedLightIn,
              combinedOverlayIn
            );
          }
        }
      } catch(Throwable e) {
        if(!broken_entities_.containsKey(te)) {
          Auxiliaries.logError("TER render error for track at " + te.getBlockPos() + ", exception: " + e.getMessage());
          Auxiliaries.logError(String.join("\n", Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).toList()));
        }
        broken_entities_.put(te, current_flags);
      } finally {
        mxs.popPose();
      }
    }
  }

}
