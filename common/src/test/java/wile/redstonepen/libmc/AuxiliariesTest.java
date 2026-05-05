package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuxiliariesTest
{
  @BeforeAll static void bootstrap() { wile.redstonepen.McBootstrap.bootstrap(); }

  @Nested
  class ModHelpers
  {
    @Test
    void modidReturnsNonEmpty()
    {
      assertNotNull(Auxiliaries.modid());
      assertFalse(Auxiliaries.modid().isEmpty());
    }

    @Test
    void loggerNotNull()
    { assertNotNull(Auxiliaries.logger()); }

    @Test
    void developmentModeFlagAccessibleAndControlFileNamed()
    {
      Auxiliaries.isDevelopmentMode();
      assertNotNull(Auxiliaries.getDevelopmentModeControlFile());
      assertTrue(Auxiliaries.getDevelopmentModeControlFile().startsWith("."));
    }

    @Test
    void logHelpersDoNotThrow()
    {
      Auxiliaries.logInfo("info-msg");
      Auxiliaries.logWarn("warn-msg");
      Auxiliaries.logError("error-msg");
      Auxiliaries.logDebug("debug-msg");
    }

    @Test
    void isModLoadedReturnsFalseForUnknownMod()
    {
      // isModLoaded delegates to PlatformServices.PLATFORM which has no ServiceLoader
      // implementation in unit tests — skip rather than fail.
      Assumptions.assumeTrue(false, "PlatformHelper not available in unit-test environment");
    }

    @Test
    @Disabled("Requires ServerLevel — exercised by GameTests")
    void particlesOnNonServerLevelReturnsEarly() {}

    @Test
    @Disabled("Requires Level/ServerLevel — exercised by GameTests")
    void getFakePlayerOnNonServerLevelReturnsEmpty() {}
  }

  @Nested
  class Localization
  {
    @Test
    void localizableSingleArgPrependsModIdForGenericKeys()
    {
      final MutableComponent c = Auxiliaries.localizable("foo");
      assertNotNull(c);
      final String key = ((net.minecraft.network.chat.contents.TranslatableContents)c.getContents()).getKey();
      assertTrue(key.startsWith(Auxiliaries.modid()+"."));
      assertTrue(key.endsWith("foo"));
    }

    @Test
    void localizableLeavesBlockAndItemKeysUntouched()
    {
      final MutableComponent block = Auxiliaries.localizable("block.foo");
      final MutableComponent item  = Auxiliaries.localizable("item.bar");
      final String bk = ((net.minecraft.network.chat.contents.TranslatableContents)block.getContents()).getKey();
      final String ik = ((net.minecraft.network.chat.contents.TranslatableContents)item.getContents()).getKey();
      assertEquals("block.foo", bk);
      assertEquals("item.bar", ik);
    }

    @Test
    void localizableWithColorAppliesFormatting()
    {
      final MutableComponent c = Auxiliaries.localizable("foo", net.minecraft.ChatFormatting.RED);
      assertNotNull(c);
      assertEquals(
        net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.RED),
        c.getStyle().getColor(),
        "RED color should be applied to component style"
      );
    }

    @Test
    void localizableBlockKeyPrefixesBlockNamespace()
    {
      final MutableComponent c = Auxiliaries.localizable_block_key("abc");
      final String key = ((net.minecraft.network.chat.contents.TranslatableContents)c.getContents()).getKey();
      assertTrue(key.startsWith("block."+Auxiliaries.modid()+"."));
    }

    @Test
    void joinSeparatorYieldsConcatenation()
    {
      final MutableComponent c = Auxiliaries.join(List.of(Component.literal("a"), Component.literal("b")), ",");
      assertEquals("a,b", c.getString());
    }

    @Test
    void joinVarargsConcatenatesWithoutSeparator()
    {
      final MutableComponent c = Auxiliaries.join(Component.literal("x"), Component.literal("y"));
      assertEquals("xy", c.getString());
    }

    @Test
    void isEmptyTrueOnlyForEmptyComponent()
    {
      assertTrue(Auxiliaries.isEmpty(Component.empty()));
      assertFalse(Auxiliaries.isEmpty(Component.literal("hi")));
    }
  }

  @Nested
  class RegistryLookups
  {
    @Test
    void resourceLocationForVanillaItemAndBlock()
    {
      assertEquals("minecraft:redstone", Auxiliaries.getResourceLocation(Items.REDSTONE).toString());
      assertEquals("minecraft:stone",
        Auxiliaries.getResourceLocation(net.minecraft.world.level.block.Blocks.STONE).toString());
    }
  }

  @Nested
  class ItemStackNbt
  {
    @Test
    void hasItemStackNbtFalseWhenAbsent()
    {
      assertFalse(Auxiliaries.hasItemStackNbt(new ItemStack(Items.REDSTONE), "anything"));
    }

    @Test
    void setAndGetItemStackNbtRoundTrips()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      final CompoundTag t = new CompoundTag();
      t.putInt("v", 7);
      Auxiliaries.setItemStackNbt(s, "key", t);
      assertTrue(Auxiliaries.hasItemStackNbt(s, "key"));
      final CompoundTag got = Auxiliaries.getItemStackNbt(s, "key");
      assertEquals(7, got.getInt("v"));
    }

    @Test
    void getItemStackNbtReturnsEmptyWhenMissing()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      final CompoundTag got = Auxiliaries.getItemStackNbt(s, "missing");
      assertNotNull(got);
      assertTrue(got.isEmpty());
    }

    @Test
    void setItemStackNbtWithEmptyKeyIsNoOp()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      Auxiliaries.setItemStackNbt(s, "", new CompoundTag());
      assertFalse(Auxiliaries.hasItemStackNbt(s, ""));
    }

    @Test
    void setItemStackNbtNullOrEmptyTagRemovesEntry()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      final CompoundTag t = new CompoundTag();
      t.putInt("v", 1);
      Auxiliaries.setItemStackNbt(s, "k", t);
      Auxiliaries.setItemStackNbt(s, "k", new CompoundTag());
      assertFalse(Auxiliaries.hasItemStackNbt(s, "k"));
      Auxiliaries.setItemStackNbt(s, "k", t);
      Auxiliaries.setItemStackNbt(s, "k", null);
      assertFalse(Auxiliaries.hasItemStackNbt(s, "k"));
    }

    @Test
    void setItemStackNbtCanReplaceExistingValue()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      final CompoundTag a = new CompoundTag(); a.putInt("v", 1);
      final CompoundTag b = new CompoundTag(); b.putInt("v", 2);
      Auxiliaries.setItemStackNbt(s, "k", a);
      Auxiliaries.setItemStackNbt(s, "k", b);
      assertEquals(2, Auxiliaries.getItemStackNbt(s, "k").getInt("v"));
    }

    @Test
    void getItemStackNbtReturnsCopyNotLiveReference()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      final CompoundTag t = new CompoundTag(); t.putInt("v", 1);
      Auxiliaries.setItemStackNbt(s, "k", t);
      final CompoundTag got = Auxiliaries.getItemStackNbt(s, "k");
      got.putInt("v", 99);
      assertEquals(1, Auxiliaries.getItemStackNbt(s, "k").getInt("v"));
    }

    @Test
    void hasItemStackNbtFalseAfterRemoval()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      Auxiliaries.setItemStackNbt(s, "k", new CompoundTag(){{ putInt("v", 1); }});
      Auxiliaries.setItemStackNbt(s, "k", new CompoundTag());
      assertFalse(Auxiliaries.hasItemStackNbt(s, "k"));
    }
  }

  @Nested
  class ItemStackLabel
  {
    @Test
    void setItemLabelStoresAndGetItemLabelReturnsIt()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      final ItemStack ret = Auxiliaries.setItemLabel(s, Component.literal("Hello"));
      assertEquals(s, ret);
      final Component label = Auxiliaries.getItemLabel(s);
      assertNotNull(label);
      assertEquals("Hello", label.getString());
    }

    @Test
    void setItemLabelNullOrBlankRemovesCustomName()
    {
      final ItemStack s = new ItemStack(Items.REDSTONE);
      Auxiliaries.setItemLabel(s, Component.literal("x"));
      Auxiliaries.setItemLabel(s, (Component)null);
      assertFalse(s.has(DataComponents.CUSTOM_NAME));
      Auxiliaries.setItemLabel(s, Component.literal("x"));
      Auxiliaries.setItemLabel(s, Component.literal("   "));
      assertFalse(s.has(DataComponents.CUSTOM_NAME));
    }
  }

  @Nested
  class BlockPosRange
  {
    @Test
    void blockPosRangeNormalizesEndpoints()
    {
      final Auxiliaries.BlockPosRange r = new Auxiliaries.BlockPosRange(5, 8, 3, 1, 2, 9);
      assertEquals(5, r.getXSize());
      assertEquals(7, r.getYSize());
      assertEquals(7, r.getZSize());
      assertEquals(5*7, r.getArea());
      assertEquals(7, r.getHeight());
      assertEquals(5*7*7, r.getVolume());
    }

    @Test
    void blockPosRangeOfAabbFloorsBoundsAndShrinksUpper()
    {
      final Auxiliaries.BlockPosRange r = Auxiliaries.BlockPosRange.of(new AABB(0.5, 1.2, 2.7, 3.5, 4.5, 5.5));
      assertEquals(0, r.byXZYIndex(0).getX());
      assertEquals(1, r.byXZYIndex(0).getY());
      assertEquals(2, r.byXZYIndex(0).getZ());
    }

    @Test
    void blockPosRangeByXZYIndexWalksXFirstThenZThenY()
    {
      final Auxiliaries.BlockPosRange r = new Auxiliaries.BlockPosRange(0, 0, 0, 1, 1, 1);
      assertEquals(new BlockPos(0,0,0), r.byXZYIndex(0));
      assertEquals(new BlockPos(1,0,0), r.byXZYIndex(1));
      assertEquals(new BlockPos(0,0,1), r.byXZYIndex(2));
      assertEquals(new BlockPos(1,0,1), r.byXZYIndex(3));
      assertEquals(new BlockPos(0,1,0), r.byXZYIndex(4));
    }

    @Test
    void blockPosRangeByXZIndexAddsYOffset()
    {
      final Auxiliaries.BlockPosRange r = new Auxiliaries.BlockPosRange(0, 0, 0, 1, 0, 1);
      assertEquals(new BlockPos(0,5,0), r.byXZIndex(0, 5));
      assertEquals(new BlockPos(1,5,0), r.byXZIndex(1, 5));
      assertEquals(new BlockPos(0,5,1), r.byXZIndex(2, 5));
    }

    @Test
    void blockPosRangeIteratorYieldsAllPositionsExactlyOnce()
    {
      final Auxiliaries.BlockPosRange r = new Auxiliaries.BlockPosRange(0, 0, 0, 1, 1, 1);
      final List<BlockPos> seen = new ArrayList<>();
      final Iterator<BlockPos> it = r.iterator();
      while(it.hasNext()) seen.add(it.next());
      assertEquals(8, seen.size());
      assertFalse(it.hasNext());
      org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void blockPosRangeStreamCountsVolume()
    {
      final Auxiliaries.BlockPosRange r = new Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 0);
      assertEquals(r.getVolume(), r.stream().count());
    }

    @Test
    void blockPosRangeVolumeEqualsProductOfDimensions()
    {
      final Auxiliaries.BlockPosRange range = new Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 1);
      assertEquals(range.getXSize() * range.getYSize() * range.getZSize(), range.getVolume());
    }

    @Test
    void blockPosRangeByXZYIndexZeroReturnsOriginCorner()
    {
      final Auxiliaries.BlockPosRange range = new Auxiliaries.BlockPosRange(1, 2, 3, 3, 4, 5);
      assertEquals(new BlockPos(1, 2, 3), range.byXZYIndex(0));
    }

    @Test
    void blockPosRangeByXZYIndexLastReturnsMaxCorner()
    {
      final Auxiliaries.BlockPosRange range = new Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 1);
      assertEquals(new BlockPos(2, 1, 1), range.byXZYIndex(range.getVolume() - 1));
    }
  }

  @Nested
  class TextSerialization
  {
    @Test
    void serializeNullComponentReturnsEmpty()
    {
      assertEquals("", Auxiliaries.serializeTextComponent(null, null));
    }
  }

  @Nested
  class ResourceLoading
  {
    @Test
    void loadResourceTextNullStreamYieldsEmpty()
    {
      assertEquals("", Auxiliaries.loadResourceText((java.io.InputStream)null));
    }

    @Test
    void loadResourceTextMissingResourceYieldsEmpty()
    {
      assertEquals("", Auxiliaries.loadResourceText("/this/path/does/not/exist.txt"));
    }

    @Test
    void loadResourceTextReadsByteContent()
    {
      final java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream("hello\nworld".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      assertEquals("hello\nworld", Auxiliaries.loadResourceText(is));
    }

    @Test
    void logGitVersionDoesNotThrow()
    { Auxiliaries.logGitVersion(); }
  }

  @Nested
  class WaterLogged
  {
    @Test
    void isWaterLoggedFalseForNonWaterLoggableBlock()
    {
      assertFalse(Auxiliaries.isWaterLogged(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
    }

    @Test
    void isWaterLoggedFalseForDryWaterLoggableBlock()
    {
      // Oak slab is water-loggable; default state is not waterlogged.
      assertFalse(Auxiliaries.isWaterLogged(net.minecraft.world.level.block.Blocks.OAK_SLAB.defaultBlockState()));
    }
  }
}
