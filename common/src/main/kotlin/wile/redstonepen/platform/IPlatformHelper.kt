package wile.redstonepen.platform

import java.nio.file.Path
import java.util.Optional
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player

interface IPlatformHelper {
    fun getGameDirectory(): Path

    fun isModLoaded(modId: String): Boolean

    fun getFakePlayer(world: ServerLevel): Optional<out Player>
}
