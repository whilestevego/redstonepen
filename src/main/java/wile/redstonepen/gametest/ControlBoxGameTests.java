package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class ControlBoxGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos CONTROL_BOX_POS = new BlockPos(1, 1, 1);

  private ControlBoxGameTests()
  {}

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void controlBoxEvaluatesConstantProgram(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity controlBox = placeControlBox(helper);
    controlBox.setCode("b=7");
    controlBox.setEnabled(true);
    controlBox.tick();

    helper.succeedWhen(() -> {
      final ControlBox.ControlBoxBlockEntity te = getControlBox(helper);
      if(te == null) throw new IllegalStateException("expected control box block entity to exist");
      if(!te.getEnabled()) throw new IllegalStateException("expected control box to be enabled");
      if(!"b=7".equals(te.getCode())) throw new IllegalStateException("expected control box code to remain applied");
      if(((te.writenbt(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag(), false)
        .getCompound("logic").getInt("output") >> (4 * net.minecraft.core.Direction.EAST.ordinal())) & 0xf) != 7) {
        throw new IllegalStateException("expected control box to compute a constant east-side output of 7");
      }
    });
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void controlBoxRejectsInvalidProgram(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity controlBox = placeControlBox(helper);
    controlBox.setCode("b=d.bad");

    helper.succeedWhen(() -> {
      final ControlBox.ControlBoxBlockEntity.TestHooks hooks = new ControlBox.ControlBoxBlockEntity.TestHooks();
      if(hooks.setCode(controlBox.getCode())) {
        throw new IllegalStateException("expected invalid control box code to remain invalid");
      }
      if(hooks.errors().isEmpty()) {
        throw new IllegalStateException("expected invalid control box code to expose parse errors");
      }
    });
  }

  private static ControlBox.ControlBoxBlockEntity placeControlBox(GameTestHelper helper)
  {
    helper.setBlock(CONTROL_BOX_POS, Registries.getBlock("control_box").defaultBlockState());
    final ControlBox.ControlBoxBlockEntity te = getControlBox(helper);
    if(te == null) throw new IllegalStateException("expected control box block entity to be created");
    return te;
  }

  private static ControlBox.ControlBoxBlockEntity getControlBox(GameTestHelper helper)
  {
    return helper.getBlockEntity(CONTROL_BOX_POS) instanceof ControlBox.ControlBoxBlockEntity te ? te : null;
  }
}
