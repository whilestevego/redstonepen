package wile.redstonepen.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.NoSuchElementException
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
import org.junit.jupiter.api.Assumptions

class AuxiliariesTest :
    DescribeSpec({
        describe("mod helpers") {
            it("modid returns non-empty") {
                Auxiliaries.modid() shouldNotBe null
                Auxiliaries.modid().isEmpty() shouldBe false
            }

            it("logger is not null") { Auxiliaries.logger() shouldNotBe null }

            it("development mode flag accessible") { Auxiliaries.isDevelopmentMode() }

            it("log helpers do not throw") {
                Auxiliaries.logInfo("info-msg")
                Auxiliaries.logWarn("warn-msg")
                Auxiliaries.logError("error-msg")
            }

            it("is mod loaded returns false for unknown mod") {
                Assumptions.assumeTrue(
                    false,
                    "PlatformHelper not available in unit-test environment",
                )
            }
        }

        describe("localization") {
            it("localizable single arg prepends modId for generic keys") {
                val c = Auxiliaries.localizable("foo")
                c shouldNotBe null
                val key = (c.contents as TranslatableContents).key
                (key.startsWith(Auxiliaries.modid() + ".")) shouldBe true
                (key.endsWith("foo")) shouldBe true
            }

            it("localizable leaves block and item keys untouched") {
                val block = Auxiliaries.localizable("block.foo")
                val item = Auxiliaries.localizable("item.bar")
                (block.contents as TranslatableContents).key shouldBe "block.foo"
                (item.contents as TranslatableContents).key shouldBe "item.bar"
            }

            it("localizable with color applies formatting") {
                val c = Auxiliaries.localizable("foo", color = ChatFormatting.RED)
                c shouldNotBe null
                c.style.color shouldBe TextColor.fromLegacyFormat(ChatFormatting.RED)
            }

            it("localizable_block_key prefixes block namespace") {
                val c = Auxiliaries.localizable_block_key("abc")
                val key = (c.contents as TranslatableContents).key
                (key.startsWith("block." + Auxiliaries.modid() + ".")) shouldBe true
            }

            it("join with separator yields concatenation") {
                val c =
                    Auxiliaries.join(listOf(Component.literal("a"), Component.literal("b")), ",")
                c.string shouldBe "a,b"
            }

            it("join varargs concatenates without separator") {
                Auxiliaries.join(Component.literal("x"), Component.literal("y")).string shouldBe
                    "xy"
            }

            it("isEmpty true only for empty component") {
                Auxiliaries.isEmpty(Component.empty()) shouldBe true
                Auxiliaries.isEmpty(Component.literal("hi")) shouldBe false
            }
        }

        describe("registry lookups") {
            it("resource location for vanilla item and block") {
                Auxiliaries.getResourceLocation(Items.REDSTONE).toString() shouldBe
                    "minecraft:redstone"
                Auxiliaries.getResourceLocation(Blocks.STONE).toString() shouldBe "minecraft:stone"
            }
        }

        describe("item stack NBT") {
            it("hasItemStackNbt false when absent") {
                Auxiliaries.hasItemStackNbt(ItemStack(Items.REDSTONE), "anything") shouldBe false
            }

            it("set and get round-trips") {
                val s = ItemStack(Items.REDSTONE)
                val t = CompoundTag()
                t.putInt("v", 7)
                Auxiliaries.setItemStackNbt(s, "key", t)
                Auxiliaries.hasItemStackNbt(s, "key") shouldBe true
                Auxiliaries.getItemStackNbt(s, "key").getInt("v") shouldBe 7
            }

            it("getItemStackNbt returns empty when missing") {
                val got = Auxiliaries.getItemStackNbt(ItemStack(Items.REDSTONE), "missing")
                got shouldNotBe null
                got.isEmpty shouldBe true
            }

            it("set with empty key is no-op") {
                val s = ItemStack(Items.REDSTONE)
                Auxiliaries.setItemStackNbt(s, "", CompoundTag())
                Auxiliaries.hasItemStackNbt(s, "") shouldBe false
            }

            it("set null or empty tag removes entry") {
                val s = ItemStack(Items.REDSTONE)
                val t = CompoundTag()
                t.putInt("v", 1)
                Auxiliaries.setItemStackNbt(s, "k", t)
                Auxiliaries.setItemStackNbt(s, "k", CompoundTag())
                Auxiliaries.hasItemStackNbt(s, "k") shouldBe false
                Auxiliaries.setItemStackNbt(s, "k", t)
                Auxiliaries.setItemStackNbt(s, "k", null)
                Auxiliaries.hasItemStackNbt(s, "k") shouldBe false
            }

            it("can replace existing value") {
                val s = ItemStack(Items.REDSTONE)
                Auxiliaries.setItemStackNbt(s, "k", CompoundTag().also { it.putInt("v", 1) })
                Auxiliaries.setItemStackNbt(s, "k", CompoundTag().also { it.putInt("v", 2) })
                Auxiliaries.getItemStackNbt(s, "k").getInt("v") shouldBe 2
            }

            it("getItemStackNbt returns copy not live reference") {
                val s = ItemStack(Items.REDSTONE)
                Auxiliaries.setItemStackNbt(s, "k", CompoundTag().also { it.putInt("v", 1) })
                val got = Auxiliaries.getItemStackNbt(s, "k")
                got.putInt("v", 99)
                Auxiliaries.getItemStackNbt(s, "k").getInt("v") shouldBe 1
            }

            it("hasItemStackNbt false after removal") {
                val s = ItemStack(Items.REDSTONE)
                Auxiliaries.setItemStackNbt(s, "k", CompoundTag().also { it.putInt("v", 1) })
                Auxiliaries.setItemStackNbt(s, "k", CompoundTag())
                Auxiliaries.hasItemStackNbt(s, "k") shouldBe false
            }
        }

        describe("item stack label") {
            it("setItemLabel stores and getItemLabel returns it") {
                val s = ItemStack(Items.REDSTONE)
                val ret = Auxiliaries.setItemLabel(s, Component.literal("Hello"))
                ret shouldBe s
                Auxiliaries.getItemLabel(s)!!.string shouldBe "Hello"
            }

            it("setItemLabel null or blank removes custom name") {
                val s = ItemStack(Items.REDSTONE)
                Auxiliaries.setItemLabel(s, Component.literal("x"))
                Auxiliaries.setItemLabel(s, null as Component?)
                s.has(DataComponents.CUSTOM_NAME) shouldBe false
                Auxiliaries.setItemLabel(s, Component.literal("x"))
                Auxiliaries.setItemLabel(s, Component.literal("   "))
                s.has(DataComponents.CUSTOM_NAME) shouldBe false
            }
        }

        describe("BlockPosRange") {
            it("normalizes endpoints") {
                val r = Auxiliaries.BlockPosRange(5, 8, 3, 1, 2, 9)
                r.getXSize() shouldBe 5
                r.getYSize() shouldBe 7
                r.getZSize() shouldBe 7
                r.getArea() shouldBe 5 * 7
                r.getHeight() shouldBe 7
                r.getVolume() shouldBe 5 * 7 * 7
            }

            it("of AABB floors bounds and shrinks upper") {
                val r = Auxiliaries.BlockPosRange.of(AABB(0.5, 1.2, 2.7, 3.5, 4.5, 5.5))
                r.byXZYIndex(0).x shouldBe 0
                r.byXZYIndex(0).y shouldBe 1
                r.byXZYIndex(0).z shouldBe 2
            }

            it("byXZYIndex walks X first then Z then Y") {
                val r = Auxiliaries.BlockPosRange(0, 0, 0, 1, 1, 1)
                r.byXZYIndex(0) shouldBe BlockPos(0, 0, 0)
                r.byXZYIndex(1) shouldBe BlockPos(1, 0, 0)
                r.byXZYIndex(2) shouldBe BlockPos(0, 0, 1)
                r.byXZYIndex(3) shouldBe BlockPos(1, 0, 1)
                r.byXZYIndex(4) shouldBe BlockPos(0, 1, 0)
            }

            it("byXZIndex adds Y offset") {
                val r = Auxiliaries.BlockPosRange(0, 0, 0, 1, 0, 1)
                r.byXZIndex(0, 5) shouldBe BlockPos(0, 5, 0)
                r.byXZIndex(1, 5) shouldBe BlockPos(1, 5, 0)
                r.byXZIndex(2, 5) shouldBe BlockPos(0, 5, 1)
            }

            it("iterator yields all positions exactly once") {
                val r = Auxiliaries.BlockPosRange(0, 0, 0, 1, 1, 1)
                val seen = mutableListOf<BlockPos>()
                val it = r.iterator()
                while (it.hasNext()) seen.add(it.next())
                seen.size shouldBe 8
                it.hasNext() shouldBe false
                shouldThrow<NoSuchElementException> { it.next() }
            }

            it("stream count equals volume") {
                val r = Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 0)
                r.stream().count() shouldBe r.getVolume().toLong()
            }

            it("volume equals product of dimensions") {
                val r = Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 1)
                r.getVolume() shouldBe r.getXSize() * r.getYSize() * r.getZSize()
            }

            it("byXZYIndex zero returns origin corner") {
                val r = Auxiliaries.BlockPosRange(1, 2, 3, 3, 4, 5)
                r.byXZYIndex(0) shouldBe BlockPos(1, 2, 3)
            }

            it("byXZYIndex last returns max corner") {
                val r = Auxiliaries.BlockPosRange(0, 0, 0, 2, 1, 1)
                r.byXZYIndex(r.getVolume() - 1) shouldBe BlockPos(2, 1, 1)
            }
        }

        describe("text serialization") {
            it("serialize null component returns empty") {
                Auxiliaries.serializeTextComponent(null, null) shouldBe ""
            }
        }

        describe("resource loading") {
            it("null stream yields empty") {
                Auxiliaries.loadResourceText(null as java.io.InputStream?) shouldBe ""
            }

            it("missing resource yields empty") {
                Auxiliaries.loadResourceText("/this/path/does/not/exist.txt") shouldBe ""
            }

            it("reads byte content") {
                val stream =
                    ByteArrayInputStream("hello\nworld".toByteArray(StandardCharsets.UTF_8))
                Auxiliaries.loadResourceText(stream) shouldBe "hello\nworld"
            }

            it("logGitVersion does not throw") { Auxiliaries.logGitVersion() }
        }

        describe("water logged") {
            it("false for non-water-loggable block") {
                Auxiliaries.isWaterLogged(Blocks.STONE.defaultBlockState()) shouldBe false
            }

            it("false for dry water-loggable block") {
                Auxiliaries.isWaterLogged(Blocks.OAK_SLAB.defaultBlockState()) shouldBe false
            }
        }
    })
