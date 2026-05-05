/*
 * @file DemoCommand.java
 * @license MIT
 *
 * Registers the /redstonepen demo command.
 */
package wile.redstonepen.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class DemoCommand
{
  private DemoCommand() {}


  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(Commands.literal("redstonepen")
      .requires(s -> s.hasPermission(2))
      .then(Commands.literal("demo").executes(ctx -> runDemo(ctx.getSource())))
    );
  }

  private static int runDemo(CommandSourceStack source)
  {
    final ServerPlayer player = source.getPlayer();
    if(player == null) {
      source.sendFailure(Component.literal("redstonepen demo requires a player source"));
      return 0;
    }
    final Level level = player.level();
    final BlockPos origin = player.blockPosition().offset(2, 0, 0);
    DemoSections.runCircuits(level, origin);
    source.sendSuccess(() -> Component.literal("redstonepen demo built at " + origin), true);
    return 1;
  }
}
