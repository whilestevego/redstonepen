package wile.redstonepen.client

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Style

class GuiTextEditingDisplayCacheTest :
    DescribeSpec({
        fun twoLineCache(): GuiTextEditing.MultiLineTextBox.DisplayCache {
            val lineStarts = intArrayOf(0, 6)
            val lines =
                arrayOf(
                    GuiTextEditing.MultiLineTextBox.LineInfo(Style.EMPTY, "hello", 0, 0),
                    GuiTextEditing.MultiLineTextBox.LineInfo(Style.EMPTY, "world", 0, 9),
                )
            return GuiTextEditing.MultiLineTextBox.DisplayCache(
                "hello\nworld",
                Guis.Coord2d(0, 0),
                false,
                lineStarts,
                lines,
                emptyArray<Rect2i>(),
            )
        }

        describe("DisplayCache.EMPTY") {
            it("has cursor at end") {
                GuiTextEditing.MultiLineTextBox.DisplayCache.EMPTY.cursorAtEnd shouldBe true
            }

            it("has cursor at origin") {
                val c = GuiTextEditing.MultiLineTextBox.DisplayCache.EMPTY.cursor
                assertSoftly(c) {
                    x shouldBe 0
                    y shouldBe 0
                }
            }
        }

        describe("changeLine") {
            it("returns unchanged pos when moving past last line") {
                twoLineCache().changeLine(7, 1) shouldBe 7
            }

            it("returns unchanged pos when moving before first line") {
                twoLineCache().changeLine(2, -1) shouldBe 2
            }

            it("moves down preserving column offset") { twoLineCache().changeLine(2, 1) shouldBe 8 }

            it("moves up preserving column offset") { twoLineCache().changeLine(9, -1) shouldBe 3 }

            it("clamps to line length") { twoLineCache().changeLine(5, 1) shouldBe 11 }
        }

        describe("findLineStart") {
            it("returns zero for first line") { twoLineCache().findLineStart(0) shouldBe 0 }

            it("returns zero for mid first line") { twoLineCache().findLineStart(3) shouldBe 0 }

            it("returns its start for second line") { twoLineCache().findLineStart(7) shouldBe 6 }
        }

        describe("findLineEnd") {
            it("returns length of contents for first line") {
                twoLineCache().findLineEnd(2) shouldBe 5
            }

            it("returns its end for second line") { twoLineCache().findLineEnd(8) shouldBe 11 }
        }
    })
