package wile.redstonepen.platform

import java.util.ServiceLoader

object PlatformServices {
    @JvmField val NETWORKING: INetworkingPlatform = load(INetworkingPlatform::class.java)

    @JvmField val PLATFORM: IPlatformHelper = load(IPlatformHelper::class.java)

    // Client-only — loaded lazily to avoid server-side class loading.
    // `by lazy {}` lambda type descriptors reference the client type at `<clinit>` time,
    // triggering NeoForge's dist-cleaner check before the lazy is ever accessed. Use
    // @Volatile backing fields instead to keep the reference out of class initialization.
    @Volatile private var networkingClient_: INetworkingClientPlatform? = null

    @Volatile private var rendering_: IRenderingPlatform? = null

    @JvmStatic
    fun getNetworkingClient(): INetworkingClientPlatform =
        networkingClient_
            ?: load(INetworkingClientPlatform::class.java).also { networkingClient_ = it }

    @JvmStatic
    fun getRendering(): IRenderingPlatform =
        rendering_ ?: load(IRenderingPlatform::class.java).also { rendering_ = it }

    private fun <T> load(clazz: Class<T>): T =
        ServiceLoader.load(clazz).findFirst().orElseThrow {
            RuntimeException("No service implementation found for: ${clazz.name}")
        }
}
