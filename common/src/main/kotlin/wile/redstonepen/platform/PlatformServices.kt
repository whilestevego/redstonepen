package wile.redstonepen.platform

import java.util.ServiceLoader

object PlatformServices {
    @JvmField val NETWORKING: INetworkingPlatform = load(INetworkingPlatform::class.java)
    @JvmField val PLATFORM: IPlatformHelper = load(IPlatformHelper::class.java)

    // Client-only — loaded lazily to avoid server-side class loading.
    // Kotlin `by lazy` lambda bootstrap runs during <clinit> and triggers class loading;
    // use nullable backing fields (null-init only) to match the original Java pattern.
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
