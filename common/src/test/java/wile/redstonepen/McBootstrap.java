package wile.redstonepen;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class McBootstrap
{
  private static boolean done = false;

  public static synchronized void bootstrap()
  {
    if(done) return;
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    done = true;
  }
}
