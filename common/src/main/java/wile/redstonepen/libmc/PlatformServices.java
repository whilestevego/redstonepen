package wile.redstonepen.libmc;

import java.util.ServiceLoader;

public class PlatformServices
{
  public static final INetworkingPlatform NETWORKING = load(INetworkingPlatform.class);
  public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

  // Client-only — loaded lazily to avoid server-side class loading
  private static INetworkingClientPlatform networkingClient;
  private static IRenderingPlatform rendering;

  public static INetworkingClientPlatform getNetworkingClient()
  {
    if(networkingClient == null) networkingClient = load(INetworkingClientPlatform.class);
    return networkingClient;
  }

  public static IRenderingPlatform getRendering()
  {
    if(rendering == null) rendering = load(IRenderingPlatform.class);
    return rendering;
  }

  private static <T> T load(Class<T> clazz)
  {
    return ServiceLoader.load(clazz).findFirst()
      .orElseThrow(() -> new RuntimeException("No service implementation found for: " + clazz.getName()));
  }
}
