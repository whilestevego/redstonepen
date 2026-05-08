package wile.redstonepen.platform

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import java.nio.file.Path
import java.util.Optional

interface IPlatformHelper {
    fun getGameDirectory(): Path
    fun isModLoaded(modId: String): Boolean
    fun getFakePlayer(world: ServerLevel): Optional<out Player>
}
