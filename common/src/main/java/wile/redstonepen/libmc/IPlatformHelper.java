package wile.redstonepen.libmc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import java.nio.file.Path;
import java.util.Optional;

public interface IPlatformHelper
{
  Path getGameDirectory();
  boolean isModLoaded(String modId);
  Optional<? extends Player> getFakePlayer(ServerLevel world);
}
