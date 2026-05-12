package wile.redstonepen

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

object McBootstrap {
    @JvmStatic
    fun bootstrap() {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
    }
}
