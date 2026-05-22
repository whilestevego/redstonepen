package wile.redstonepen.fabric

import io.kotest.core.config.AbstractProjectConfig
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

object KotestProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
    }
}
