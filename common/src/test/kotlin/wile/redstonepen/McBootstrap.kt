package wile.redstonepen

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

object McBootstrap {
    private var done = false

    @JvmStatic
    @Synchronized
    fun bootstrap() {
        if (done) return
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
        done = true
    }
}
