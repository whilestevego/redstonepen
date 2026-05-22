package wile.redstonepen.blocks.basic

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BasicConfigTest :
    DescribeSpec({
        describe("BasicButtonConfig") {
            it("stores all three fields") {
                val cfg =
                    BasicButtonConfig(
                        soundPitchUnpowered = 0.8f,
                        soundPitchPowered = 1.2f,
                        activeTime = 20,
                    )
                assertSoftly {
                    cfg.soundPitchUnpowered shouldBe 0.8f
                    cfg.soundPitchPowered shouldBe 1.2f
                    cfg.activeTime shouldBe 20
                }
            }

            it("copy changes only the specified field") {
                val original = BasicButtonConfig(0.8f, 1.2f, 20)
                val modified = original.copy(activeTime = 40)
                assertSoftly {
                    modified.activeTime shouldBe 40
                    modified.soundPitchUnpowered shouldBe original.soundPitchUnpowered
                    modified.soundPitchPowered shouldBe original.soundPitchPowered
                }
            }

            it("equal when all fields match") {
                BasicButtonConfig(0.5f, 1.0f, 10) shouldBe BasicButtonConfig(0.5f, 1.0f, 10)
            }

            it("not equal when any field differs") {
                BasicButtonConfig(0.5f, 1.0f, 10) shouldNotBe BasicButtonConfig(0.5f, 1.0f, 20)
            }
        }

        describe("BasicLeverConfig") {
            it("stores both fields") {
                val cfg = BasicLeverConfig(soundPitchUnpowered = 0.9f, soundPitchPowered = 1.1f)
                assertSoftly {
                    cfg.soundPitchUnpowered shouldBe 0.9f
                    cfg.soundPitchPowered shouldBe 1.1f
                }
            }

            it("copy changes only the specified field") {
                val original = BasicLeverConfig(0.9f, 1.1f)
                val modified = original.copy(soundPitchPowered = 1.5f)
                assertSoftly {
                    modified.soundPitchPowered shouldBe 1.5f
                    modified.soundPitchUnpowered shouldBe original.soundPitchUnpowered
                }
            }

            it("equal when both fields match") {
                BasicLeverConfig(0.9f, 1.1f) shouldBe BasicLeverConfig(0.9f, 1.1f)
            }

            it("not equal when a field differs") {
                BasicLeverConfig(0.9f, 1.1f) shouldNotBe BasicLeverConfig(0.9f, 1.2f)
            }
        }
    })
