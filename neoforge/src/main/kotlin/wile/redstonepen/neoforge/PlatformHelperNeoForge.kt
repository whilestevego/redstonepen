package wile.redstonepen.neoforge

import java.nio.file.Path
import java.util.Optional
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforge.common.util.FakePlayerFactory
import wile.redstonepen.platform.IPlatformHelper

class PlatformHelperNeoForge : IPlatformHelper {
    override fun getGameDirectory(): Path = FMLLoader.getGamePath()

    override fun isModLoaded(modId: String): Boolean = ModList.get().isLoaded(modId)

    override fun getFakePlayer(world: ServerLevel): Optional<out Player> =
        try {
            Optional.of(FakePlayerFactory.getMinecraft(world))
        } catch (ignored: Exception) {
            Optional.empty()
        }
}
