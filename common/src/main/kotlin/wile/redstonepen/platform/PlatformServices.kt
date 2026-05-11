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

    @JvmStatic fun getNetworkingClient(): INetworkingClientPlatform {
        if (networkingClient_ == null) networkingClient_ = load(INetworkingClientPlatform::class.java)
        return networkingClient_!!
    }

    @JvmStatic fun getRendering(): IRenderingPlatform {
        if (rendering_ == null) rendering_ = load(IRenderingPlatform::class.java)
        return rendering_!!
    }

    private fun <T> load(clazz: Class<T>): T = ServiceLoader.load(clazz).findFirst()
        .orElseThrow { RuntimeException("No service implementation found for: ${clazz.name}") }
}
