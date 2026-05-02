/*
 * @file DemoCommand.java
 * @license MIT
 *
 * Registers the /redstonepen demo command.
 */
package wile.redstonepen.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DemoCommand
{
  private DemoCommand() {}

  public static void onRegisterCommands(RegisterCommandsEvent event)
  {
    register(event.getDispatcher());
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("redstonepen")
      .requires(s -> s.hasPermission(2))
      .then(Commands.literal("demo")
        .executes(ctx -> runSection(ctx.getSource(), Section.CIRCUITS))
        .then(Commands.literal("circuits").executes(ctx -> runSection(ctx.getSource(), Section.CIRCUITS)))
        .then(Commands.literal("all").executes(ctx -> runSection(ctx.getSource(), Section.ALL)))
      );
    dispatcher.register(root);
  }

  private enum Section { CIRCUITS, ALL }

  private static int runSection(CommandSourceStack source, Section section)
  {
    final ServerPlayer player = source.getPlayer();
    if(player == null) {
      source.sendFailure(Component.literal("redstonepen demo requires a player source"));
      return 0;
    }
    final Level level = player.level();
    final BlockPos origin = player.blockPosition().offset(2, 0, 0);
    switch(section) {
      case CIRCUITS -> DemoSections.runCircuits(level, origin);
      case ALL -> DemoSections.runAll(level, origin);
    }
    source.sendSuccess(() -> Component.literal("redstonepen demo: " + section.name().toLowerCase() + " built at " + origin), true);
    return 1;
  }
}
