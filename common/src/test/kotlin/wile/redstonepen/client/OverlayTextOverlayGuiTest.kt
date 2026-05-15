package wile.redstonepen.client

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.minecraft.network.chat.Component

class OverlayTextOverlayGuiTest :
    DescribeSpec({
        beforeEach { Overlay.TextOverlayGui.hide() }

        describe("hide") {
            it("zeros deadline and clears text") {
                Overlay.TextOverlayGui.show("hello", 5000)
                Overlay.TextOverlayGui.hide()
                assertSoftly {
                    Overlay.TextOverlayGui.deadline() shouldBe 0
                    Overlay.TextOverlayGui.text() shouldBe Overlay.TextOverlayGui.EMPTY_TEXT
                }
            }
        }

        describe("show string") {
            it("updates text to match input") {
                Overlay.TextOverlayGui.show("ping", 1000)
                Overlay.TextOverlayGui.text().string shouldBe "ping"
            }

            it("null string falls back to empty text") {
                Overlay.TextOverlayGui.show(null as String?, 1000)
                Overlay.TextOverlayGui.text() shouldBe Overlay.TextOverlayGui.EMPTY_TEXT
            }

            it("empty string falls back to empty text") {
                Overlay.TextOverlayGui.show("", 1000)
                Overlay.TextOverlayGui.text() shouldBe Overlay.TextOverlayGui.EMPTY_TEXT
            }

            it("sets deadline in future") {
                val before = System.currentTimeMillis()
                Overlay.TextOverlayGui.show("x", 500)
                (Overlay.TextOverlayGui.deadline() >= before + 500) shouldBe true
            }
        }

        describe("show component") {
            it("updates text to match input") {
                Overlay.TextOverlayGui.show(Component.literal("world"), 1000)
                Overlay.TextOverlayGui.text().string shouldBe "world"
            }

            it("null component falls back to empty text") {
                Overlay.TextOverlayGui.show(null as Component?, 1000)
                Overlay.TextOverlayGui.text() shouldBe Overlay.TextOverlayGui.EMPTY_TEXT
            }

            it("sets deadline in future") {
                val before = System.currentTimeMillis()
                Overlay.TextOverlayGui.show(Component.literal("x"), 1000)
                (Overlay.TextOverlayGui.deadline() >= before + 1000) shouldBe true
            }
        }
    })
