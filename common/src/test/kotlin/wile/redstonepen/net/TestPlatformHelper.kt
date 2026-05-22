package wile.redstonepen.net

import java.nio.file.Path
import java.util.Optional
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import wile.redstonepen.platform.IPlatformHelper

class TestPlatformHelper : IPlatformHelper {
    override fun getGameDirectory(): Path = Path.of(".")

    override fun isModLoaded(modId: String): Boolean = false

    override fun getFakePlayer(world: ServerLevel): Optional<out Player> = Optional.empty()
}
