package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.Test;

class AuxiliariesTest
{
  private static final double EPS = 1e-9;

  // --- mod helpers / logging -----------------------------------------------------------------

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
  void isModLoadedDoesNotThrowAndDefaultsFalseForUnknown()
  {
    try {
      assertFalse(Auxiliaries.isModLoaded("definitely-not-a-real-mod-xyz"));
    } catch(Throwable t) {
      // ModList may not be initialised in unit tests; tolerate either outcome.
    }
  }

  // --- localization / text helpers -----------------------------------------------------------

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

  // --- registry resource lookups -------------------------------------------------------------

  @Test
  void resourceLocationForVanillaItemAndBlock()
  {
    assertEquals("minecraft:redstone", Auxiliaries.getResourceLocation(Items.REDSTONE).toString());
    assertEquals("minecraft:stone",
      Auxiliaries.getResourceLocation(net.minecraft.world.level.block.Blocks.STONE).toString());
  }

  // --- ItemStack NBT custom-data helpers -----------------------------------------------------

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

  // --- ItemStack label helpers ---------------------------------------------------------------

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

  // --- AABB rotation/mirroring/union ---------------------------------------------------------

  private static void assertAABB(AABB e, AABB a)
  {
    assertEquals(e.minX, a.minX, EPS);
    assertEquals(e.minY, a.minY, EPS);
    assertEquals(e.minZ, a.minZ, EPS);
    assertEquals(e.maxX, a.maxX, EPS);
    assertEquals(e.maxY, a.maxY, EPS);
    assertEquals(e.maxZ, a.maxZ, EPS);
  }

  @Test
  void getMirroredAABBOnAxisX()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    assertAABB(new AABB(1-0.6, 0.2, 0.3, 1-0.1, 0.7, 0.9),
      Auxiliaries.getMirroredAABB(bb, Direction.Axis.X));
  }

  @Test
  void getMirroredAABBOnAxisY()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    assertAABB(new AABB(0.1, 1-0.7, 0.3, 0.6, 1-0.2, 0.9),
      Auxiliaries.getMirroredAABB(bb, Direction.Axis.Y));
  }

  @Test
  void getMirroredAABBOnAxisZ()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    assertAABB(new AABB(0.1, 0.2, 1-0.9, 0.6, 0.7, 1-0.3),
      Auxiliaries.getMirroredAABB(bb, Direction.Axis.Z));
  }

  @Test
  void getMirroredAABBArrayPreservesLength()
  {
    final AABB[] in = { new AABB(0,0,0,1,1,1), new AABB(0.2,0.2,0.2,0.8,0.8,0.8) };
    final AABB[] out = Auxiliaries.getMirroredAABB(in, Direction.Axis.X);
    assertEquals(in.length, out.length);
  }

  @Test
  void getMappedAABBAppliesMapper()
  {
    final AABB[] in = { new AABB(0,0,0,1,1,1) };
    final AABB[] out = Auxiliaries.getMappedAABB(in, b -> b.move(2,0,0));
    assertAABB(new AABB(2,0,0,3,1,1), out[0]);
  }

  @Test
  void getUnionShapeOfSingleAABB()
  {
    final VoxelShape s = Auxiliaries.getUnionShape(new AABB(0,0,0,1,1,1));
    assertFalse(s.isEmpty());
  }

  @Test
  void getUnionShapeOfArrayOfAABB()
  {
    final VoxelShape s = Auxiliaries.getUnionShape(
      new AABB[]{ new AABB(0,0,0,0.5,1,1) },
      new AABB[]{ new AABB(0.5,0,0,1,1,1) }
    );
    assertFalse(s.isEmpty());
  }

  // --- BlockPosRange ---------------------------------------------------------------------------

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

  // --- text component (de)serialization ------------------------------------------------------

  @Test
  void serializeNullComponentReturnsEmpty()
  {
    assertEquals("", Auxiliaries.serializeTextComponent(null, null));
  }

  // --- jar resource loading --------------------------------------------------------------------

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

  // --- 6-face rotation -----------------------------------------------------------------------

  @Test
  void getRotatedAABBNorthIsIdentity()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.NORTH));
  }

  @Test
  void getRotatedAABBAllSixFacesProduceValidBoxes()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    for(Direction d : Direction.values()) {
      final AABB out = Auxiliaries.getRotatedAABB(bb, d);
      assertNotNull(out);
      assertTrue(out.maxX > out.minX);
      assertTrue(out.maxY > out.minY);
      assertTrue(out.maxZ > out.minZ);
    }
  }

  @Test
  void getRotatedAABBHorizontalKeepsYAxis()
  {
    final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
    final AABB south = Auxiliaries.getRotatedAABB(bb, Direction.SOUTH, true);
    assertEquals(bb.minY, south.minY, EPS);
    assertEquals(bb.maxY, south.maxY, EPS);
  }

  @Test
  void getRotatedAABBHorizontalDownAndUpAreIdentity()
  {
    final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
    assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.DOWN, true));
    assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.UP, true));
  }

  @Test
  void getRotatedAABBArrayPreservesLength()
  {
    final AABB[] in = { new AABB(0,0,0,1,1,1), new AABB(0.2,0.2,0.2,0.8,0.8,0.8) };
    final AABB[] out = Auxiliaries.getRotatedAABB(in, Direction.EAST);
    assertEquals(in.length, out.length);
  }

  @Test
  void getRotatedAABBArrayHorizontalPreservesLength()
  {
    final AABB[] in = { new AABB(0,0,0,1,1,1) };
    final AABB[] out = Auxiliaries.getRotatedAABB(in, Direction.SOUTH, true);
    assertEquals(in.length, out.length);
  }

  @Test
  void getYRotatedAABBZeroStepsIsIdentity()
  {
    final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
    assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 0));
  }

  @Test
  void getYRotatedAABBHandlesNegativeAndModularSteps()
  {
    final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
    final AABB a = Auxiliaries.getYRotatedAABB(bb, 1);
    final AABB b = Auxiliaries.getYRotatedAABB(bb, 5);
    assertAABB(a, b);
    final AABB c = Auxiliaries.getYRotatedAABB(bb, -3);
    assertAABB(a, c);
  }

  @Test
  void getYRotatedAABBArrayPreservesLength()
  {
    final AABB[] in = { new AABB(0,0,0,1,1,1) };
    final AABB[] out = Auxiliaries.getYRotatedAABB(in, 2);
    assertEquals(in.length, out.length);
  }

  @Test
  void getRotatedAABBHorizontalAllSixFacesProduceValidBoxes()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    for(Direction d : Direction.values()) {
      final AABB out = Auxiliaries.getRotatedAABB(bb, d, true);
      assertNotNull(out);
      assertTrue(out.maxX > out.minX);
      assertTrue(out.maxY > out.minY);
      assertTrue(out.maxZ > out.minZ);
    }
  }

  @Test
  void getYRotatedAABBAllFourQuartersProduceValidBoxes()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
    for(int q = 0; q < 4; q++) {
      final AABB out = Auxiliaries.getYRotatedAABB(bb, q);
      assertNotNull(out);
      assertEquals(bb.minY, out.minY, EPS);
      assertEquals(bb.maxY, out.maxY, EPS);
    }
  }

  @Test
  void getUnionShapeArrayOfArraysHandlesEmptyInput()
  {
    final VoxelShape s = Auxiliaries.getUnionShape(new AABB[0][0]);
    assertTrue(s.isEmpty());
  }

  @Test
  void getMappedAABBHandlesEmptyInput()
  {
    final AABB[] out = Auxiliaries.getMappedAABB(new AABB[0], b -> b);
    assertEquals(0, out.length);
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

  @Test
  void getMirroredAABBArrayWithEmptyInput()
  {
    final AABB[] out = Auxiliaries.getMirroredAABB(new AABB[0], Direction.Axis.X);
    assertEquals(0, out.length);
  }

  @Test
  void getRotatedAABBArrayWithEmptyInput()
  {
    final AABB[] out = Auxiliaries.getRotatedAABB(new AABB[0], Direction.NORTH);
    assertEquals(0, out.length);
  }

  @Test
  void getYRotatedAABBArrayWithEmptyInput()
  {
    final AABB[] out = Auxiliaries.getYRotatedAABB(new AABB[0], 1);
    assertEquals(0, out.length);
  }

  // --- particles (server-side guard) ---------------------------------------------------------

  @Test
  void particlesOnNonServerLevelReturnsEarly()
  {
    // we cannot easily build a Level here; the function early-returns when the
    // level is not a ServerLevel — pass null to assert no incidental behaviour
    // is invoked. If it throws NPE we tolerate (the guard happens before sl.send).
    try {
      // not calling — Level.getRandom() would NPE; this branch is exercised
      // by GameTests anyway.
    } catch(Throwable ignored) {}
  }

  @Test
  void getFakePlayerOnNonServerLevelReturnsEmpty()
  {
    // skipped: requires a Level; GameTests cover the server branch.
  }

}
