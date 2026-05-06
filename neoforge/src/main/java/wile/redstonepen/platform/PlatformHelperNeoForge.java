package wile.redstonepen.platform;

import wile.redstonepen.libmc.*;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.fml.ModList;

import java.nio.file.Path;
import java.util.Optional;

public class PlatformHelperNeoForge implements IPlatformHelper
{
  @Override
  public Path getGameDirectory()
  { return FMLLoader.getGamePath(); }

  @Override
  public boolean isModLoaded(String modId)
  { return ModList.get().isLoaded(modId); }

  @Override
  public Optional<? extends Player> getFakePlayer(ServerLevel world)
  {
    try { return Optional.of(FakePlayerFactory.getMinecraft(world)); }
    catch(Exception e) { return Optional.empty(); }
  }
}
