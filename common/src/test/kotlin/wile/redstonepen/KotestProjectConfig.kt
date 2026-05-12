package wile.redstonepen

import io.kotest.core.config.AbstractProjectConfig

object KotestProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        McBootstrap.bootstrap()
    }
}
