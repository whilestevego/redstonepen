package wile.redstonepen.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import wile.redstonepen.libmc.IPlatformHelper;

import java.nio.file.Path;
import java.util.Optional;

public class PlatformHelperFabric implements IPlatformHelper
{
  @Override
  public Path getGameDirectory()
  { return FabricLoader.getInstance().getGameDir(); }

  @Override
  public boolean isModLoaded(String modId)
  { return FabricLoader.getInstance().isModLoaded(modId); }

  @Override
  public Optional<? extends Player> getFakePlayer(ServerLevel world)
  {
    try {
      var player = net.fabricmc.fabric.api.entity.FakePlayer.get(world);
      return (player == null) ? Optional.empty() : Optional.of(player);
    } catch(Exception e) {
      return Optional.empty();
    }
  }
}
