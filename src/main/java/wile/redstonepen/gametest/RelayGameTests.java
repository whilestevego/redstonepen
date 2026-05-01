package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class RelayGameTests
{
  private RelayGameTests()
  {}

  @GameTest(templateNamespace = "minecraft", template = "relay_activates_from_redstone", timeoutTicks = 20)
  public static void relayActivatesFromRedstone(GameTestHelper helper)
  {
    final BlockPos relayPos = new BlockPos(1, 1, 1);
    final BlockPos inputPos = relayPos.east();

    helper.setBlock(inputPos, Blocks.REDSTONE_BLOCK);
    helper.setBlock(relayPos, Registries.getBlock("relay").defaultBlockState());

    helper.succeedWhen(() -> helper.assertBlockState(
      relayPos,
      state -> state.is(Registries.getBlock("relay"))
        && state.getValue(CircuitComponents.DirectedComponentBlock.POWERED),
      () -> "expected relay to become powered from adjacent redstone input"
    ));
  }
}
