package wile.redstonepen.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component


object DemoCommand {

    @JvmStatic fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("redstonepen")
            .requires { s -> s.hasPermission(2) }
            .then(Commands.literal("demo").executes { ctx -> runDemo(ctx.source) })
        )
    }

    private fun runDemo(source: CommandSourceStack): Int {
        val player = source.player ?: run {
            source.sendFailure(Component.literal("redstonepen demo requires a player source"))
            return 0
        }
        val origin = player.blockPosition().offset(2, 0, 0)
        DemoSections.runCircuits(player.level(), origin)
        source.sendSuccess({ Component.literal("redstonepen demo built at $origin") }, true)
        return 1
    }
}
