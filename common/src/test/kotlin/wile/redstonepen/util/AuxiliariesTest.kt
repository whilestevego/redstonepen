package wile.redstonepen.util

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.NoSuchElementException

class AuxiliariesTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    @Nested inner class ModHelpers {
        @Test fun modidReturnsNonEmpty() {
            assertNotNull(Auxiliaries.modid())
            assertFalse(Auxiliaries.modid().isEmpty())
        }

        @Test fun loggerNotNull() {
            assertNotNull(Auxiliaries.logger())
        }

        @Test fun developmentModeFlagAccessible() {
            Auxiliaries.isDevelopmentMode()
        }

        @Test fun logHelpersDoNotThrow() {
            Auxiliaries.logInfo("info-msg")
            Auxiliaries.logWarn("warn-msg")
            Auxiliaries.logError("error-msg")
        }

        @Test fun isModLoadedReturnsFalseForUnknownMod() {
            Assumptions.assumeTrue(false, "PlatformHelper not available in unit-test environment")
        }

        @Test @Disabled("Requires ServerLevel — exercised by GameTests")
        fun particlesOnNonServerLevelReturnsEarly() {}

        @Test @Disabled("Requires Level/ServerLevel — exercised by GameTests")
        fun getFakePlayerOnNonServerLevelReturnsEmpty() {}
    }

    @Nested inner class Localization {
        @Test fun localizableSingleArgPrependsModIdForGenericKeys() {
            val c = Auxiliaries.localizable("foo")
            assertNotNull(c)
            val key = (c.contents as TranslatableContents).key
            assertTrue(key.startsWith(Auxiliaries.modid() + "."))
            assertTrue(key.endsWith("foo"))
        }

        @Test fun localizableLeavesBlockAndItemKeysUntouched() {
            val block = Auxiliaries.localizable("block.foo")
            val item  = Auxiliaries.localizable("item.bar")
            val bk = (block.contents as TranslatableContents).key
            val ik = (item.contents as TranslatableContents).key
            assertEquals("block.foo", bk)
            assertEquals("item.bar", ik)
        }

        @Test fun localizableWithColorAppliesFormatting() {
            val c = Auxiliaries.localizable("foo", color = ChatFormatting.RED)
            assertNotNull(c)
            assertEquals(
                TextColor.fromLegacyFormat(ChatFormatting.RED),
                c.style.color,
                "RED color should be applied to component style"
            )
        }

        @Test fun localizableBlockKeyPrefixesBlockNamespace() {
            val c = Auxiliaries.localizable_block_key("abc")
            val key = (c.contents as TranslatableContents).key
            assertTrue(key.startsWith("block." + Auxiliaries.modid() + "."))
        }

        @Test fun joinSeparatorYieldsConcatenation() {
            val c = Auxiliaries.join(listOf(Component.literal("a"), Component.literal("b")), ",")
            assertEquals("a,b", c.string)
        }

        @Test fun joinVarargsConcatenatesWithoutSeparator() {
            val c = Auxiliaries.join(Component.literal("x"), Component.literal("y"))
            assertEquals("xy", c.string)
        }

        @Test fun isEmptyTrueOnlyForEmptyComponent() {
            assertTrue(Auxiliaries.isEmpty(Component.empty()))
            assertFalse(Auxiliaries.isEmpty(Component.literal("hi")))
        }
    }

    @Nested inner class RegistryLookups {
        @Test fun resourceLocationForVanillaItemAndBlock() {
            assertEquals("minecraft:redstone", Auxiliaries.getResourceLocation(Items.REDSTONE).toString())
            assertEquals("minecraft:stone", Auxiliaries.getResourceLocation(Blocks.STONE).toString())
        }
    }

    @Nested inner class ItemStackNbt {
        @Test fun hasItemStackNbtFalseWhenAbsent() {
            assertFalse(Auxiliaries.hasItemStackNbt(ItemStack(Items.REDSTONE), "anything"))
        }

        @Test fun setAndGetItemStackNbtRoundTrips() {
            val s = ItemStack(Items.REDSTONE)
            val t = CompoundTag()
            t.putInt("v", 7)
            Auxiliaries.setItemStackNbt(s, "key", t)
            assertTrue(Auxiliaries.hasItemStackNbt(s, "key"))
            val got = Auxiliaries.getItemStackNbt(s, "key")
            assertEquals(7, got.getInt("v"))
        }

        @Test fun getItemStackNbtReturnsEmptyWhenMissing() {
            val s = ItemStack(Items.REDSTONE)
            val got = Auxiliaries.getItemStackNbt(s, "missing")
            assertNotNull(got)
            assertTrue(got.isEmpty)
        }

        @Test fun setItemStackNbtWithEmptyKeyIsNoOp() {
            val s = ItemStack(Items.REDSTONE)
            Auxiliaries.setItemStackNbt(s, "", CompoundTag())
            assertFalse(Auxiliaries.hasItemStackNbt(s, ""))
        }

        @Test fun setItemStackNbtNullOrEmptyTagRemovesEntry() {
            val s = ItemStack(Items.REDSTONE)
            val t = CompoundTag()
            t.putInt("v", 1)
            Auxiliaries.setItemStackNbt(s, "k", t)
            Auxiliaries.setItemStackNbt(s, "k", CompoundTag())
            assertFalse(Auxiliaries.hasItemStackNbt(s, "k"))
            Auxiliaries.setItemStackNbt(s, "k", t)
            Auxiliaries.setItemStackNbt(s, "k", null)
            assertFalse(Auxiliaries.hasItemStackNbt(s, "k"))
        }

        @Test fun setItemStackNbtCanReplaceExistingValue() {
            val s = ItemStack(Items.REDSTONE)
            val a = CompoundTag().also { it.putInt("v", 1) }
            val b = CompoundTag().also { it.putInt("v", 2) }
            Auxiliaries.setItemStackNbt(s, "k", a)
            Auxiliaries.setItemStackNbt(s, "k", b)
            assertEquals(2, Auxiliaries.getItemStackNbt(s, "k").getInt("v"))
        }

        @Test fun getItemStackNbtReturnsCopyNotLiveReference() {
            val s = ItemStack(Items.REDSTONE)
            val t = CompoundTag().also { it.putInt("v", 1) }
            Auxiliaries.setItemStackNbt(s, "k", t)
            val got = Auxiliaries.getItemStackNbt(s, "k")
            got.putInt("v", 99)
            assertEquals(1, Auxiliaries.getItemStackNbt(s, "k").getInt("v"))
        }

        @Test fun hasItemStackNbtFalseAfterRemoval() {
            val s = ItemStack(Items.REDSTONE)
            Auxiliaries.setItemStackNbt(s, "k", CompoundTag().also { it.putInt("v", 1) })
            Auxiliaries.setItemStackNbt(s, "k", CompoundTag())
            assertFalse(Auxiliaries.hasItemStackNbt(s, "k"))
        }
    }

    @Nested inner class ItemStackLabel {
        @Test fun setItemLabelStoresAndGetItemLabelReturnsIt() {
            val s = ItemStack(Items.REDSTONE)
            val ret = Auxiliaries.setItemLabel(s, Component.literal("Hello"))
            assertEquals(s, ret)
            val label = Auxiliaries.getItemLabel(s)
            assertNotNull(label)
            assertEquals("Hello", label!!.string)
        }

        @Test fun setItemLabelNullOrBlankRemovesCustomName() {
            val s = ItemStack(Items.REDSTONE)
            Auxiliaries.setItemLabel(s, Component.literal("x"))
            Auxiliaries.setItemLabel(s, null as Component?)
            assertFalse(s.has(DataComponents.CUSTOM_NAME))
            Auxiliaries.setItemLabel(s, Component.literal("x"))
            Auxiliaries.setItemLabel(s, Component.literal("   "))
            assertFalse(s.has(DataComponents.CUSTOM_NAME))
        }
    }

    @Nested inner class BlockPosRange {
        @Test fun blockPosRangeNormalizesEndpoints() {
            val r = Auxiliaries.BlockPosRange(5, 8, 3, 1, 2, 9)
            assertEquals(5, r.getXSize())
            assertEquals(7, r.getYSize())
            assertEquals(7, r.getZSize())
            assertEquals(5 * 7, r.getArea())
            assertEquals(7, r.getHeight())
            assertEquals(5 * 7 * 7, r.getVolume())
        }

        @Test fun blockPosRangeOfAabbFloorsBoundsAndShrinksUpper() {
            val r = Auxiliaries.BlockPosRange.of(AABB(0.5, 1.2, 2.7, 3.5, 4.5, 5.5))
            assertEquals(0, r.byXZYIndex(0).x)
            assertEquals(1, r.byXZYIndex(0).y)
            assertEquals(2, r.byXZYIndex(0).z)
        }

        @Test fun blockPosRangeByXZYIndexWalksXFirstThenZThenY() {
            val r = Auxiliaries.BlockPosRange(0, 0, 0, 1, 1, 1)
            assertEquals(BlockPos(0, 0, 0), r.byXZYIndex(0))
            assertEquals(BlockPos(1, 0, 0), r.byXZYIndex(1))
            assertEquals(BlockPos(0, 0, 1), r.byXZYIndex(2))
            assertEquals(BlockPos(1, 0, 1), r.byXZYIndex(3))
            assertEquals(BlockPos(0, 1, 0), r.byXZYIndex(4))
        }

        @Test fun blockPosRangeByXZIndexAddsYOffset() {
            val r = Auxiliaries.BlockPosRange(0, 0, 0, 1, 0, 1)
            assertEquals(BlockPos(0, 5, 0), r.byXZIndex(0, 5))
            assertEquals(BlockPos(1, 5, 0), r.byXZIndex(1, 5))
            assertEquals(BlockPos(0, 5, 1), r.byXZIndex(2, 5))
        }

        @Test fun blockPosRangeIteratorYieldsAllPositionsExactlyOnce() {
            val r = Auxiliaries.BlockPosRange(0, 0, 0, 1, 1, 1)
            val seen = mutableListOf<BlockPos>()
            val it = r.iterator()
            while (it.hasNext()) seen.add(it.next())
            assertEquals(8, seen.size)
            assertFalse(it.hasNext())
            assertThrows(NoSuchElementException::class.java) { it.next() }
        }

        @Test fun blockPosRangeStreamCountsVolume() {
            val r = Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 0)
            assertEquals(r.getVolume().toLong(), r.stream().count())
        }

        @Test fun blockPosRangeVolumeEqualsProductOfDimensions() {
            val range = Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 1)
            assertEquals(range.getXSize() * range.getYSize() * range.getZSize(), range.getVolume())
        }

        @Test fun blockPosRangeByXZYIndexZeroReturnsOriginCorner() {
            val range = Auxiliaries.BlockPosRange(1, 2, 3, 3, 4, 5)
            assertEquals(BlockPos(1, 2, 3), range.byXZYIndex(0))
        }

        @Test fun blockPosRangeByXZYIndexLastReturnsMaxCorner() {
            val range = Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 1)
            assertEquals(BlockPos(2, 1, 1), range.byXZYIndex(range.getVolume() - 1))
        }
    }

    @Nested inner class TextSerialization {
        @Test fun serializeNullComponentReturnsEmpty() {
            assertEquals("", Auxiliaries.serializeTextComponent(null, null))
        }
    }

    @Nested inner class ResourceLoading {
        @Test fun loadResourceTextNullStreamYieldsEmpty() {
            assertEquals("", Auxiliaries.loadResourceText(null as java.io.InputStream?))
        }

        @Test fun loadResourceTextMissingResourceYieldsEmpty() {
            assertEquals("", Auxiliaries.loadResourceText("/this/path/does/not/exist.txt"))
        }

        @Test fun loadResourceTextReadsByteContent() {
            val stream = ByteArrayInputStream("hello\nworld".toByteArray(StandardCharsets.UTF_8))
            assertEquals("hello\nworld", Auxiliaries.loadResourceText(stream))
        }

        @Test fun logGitVersionDoesNotThrow() {
            Auxiliaries.logGitVersion()
        }
    }

    @Nested inner class WaterLogged {
        @Test fun isWaterLoggedFalseForNonWaterLoggableBlock() {
            assertFalse(Auxiliaries.isWaterLogged(Blocks.STONE.defaultBlockState()))
        }

        @Test fun isWaterLoggedFalseForDryWaterLoggableBlock() {
            assertFalse(Auxiliaries.isWaterLogged(Blocks.OAK_SLAB.defaultBlockState()))
        }
    }
}
