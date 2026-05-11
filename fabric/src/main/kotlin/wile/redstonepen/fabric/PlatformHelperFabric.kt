package wile.redstonepen.fabric

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import wile.redstonepen.platform.IPlatformHelper
import java.nio.file.Path
import java.util.Optional

class PlatformHelperFabric : IPlatformHelper {
    override fun getGameDirectory(): Path = FabricLoader.getInstance().gameDir

    override fun isModLoaded(modId: String): Boolean = FabricLoader.getInstance().isModLoaded(modId)

    override fun getFakePlayer(world: ServerLevel): Optional<out Player> = try {
        val player = net.fabricmc.fabric.api.entity.FakePlayer.get(world)
        if (player == null) Optional.empty() else Optional.of(player)
    } catch (e: Exception) {
        Optional.empty()
    }
}
