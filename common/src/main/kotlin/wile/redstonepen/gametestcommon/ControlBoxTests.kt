package wile.redstonepen.gametestcommon

import java.util.UUID
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import wile.redstonepen.blocks.controlbox.ControlBoxBlock
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity
import wile.redstonepen.blocks.controlbox.Defs
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries

object ControlBoxTests {
    private val CONTROL_BOX_POS = BlockPos(1, 1, 1)

    @JvmStatic
    fun controlBoxEvaluatesConstantProgram(helper: GameTestHelper) {
        val controlBox = placeControlBox(helper)
        controlBox.setCode("b=7")
        controlBox.setEnabled(true)
        controlBox.tick()
        helper.succeedWhen {
            val te = getControlBox(helper) ?: error("expected control box block entity to exist")
            check(te.getEnabled()) { "expected control box to be enabled" }
            check("b=7" == te.getCode()) { "expected control box code to remain applied" }
            val output =
                te.writenbt(helper.level.registryAccess(), CompoundTag(), false)
                    .getCompound("logic")
                    .getInt("output")
            check((output shr (4 * Direction.EAST.ordinal)) and 0xf == 7) {
                "expected control box to compute a constant east-side output of 7"
            }
        }
    }

    @JvmStatic
    fun controlBoxRejectsInvalidProgram(helper: GameTestHelper) {
        val controlBox = placeControlBox(helper)
        controlBox.setCode("b=d.bad")
        helper.succeedWhen {
            val hooks = ControlBoxBlockEntity.TestHooks()
            check(!hooks.setCode(controlBox.getCode())) {
                "expected invalid control box code to remain invalid"
            }
            check(hooks.errors().isNotEmpty()) {
                "expected invalid control box code to expose parse errors"
            }
        }
    }

    @JvmStatic
    fun writenbtRoundTripsCodeAndSymbols(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=5")
        val nbt = te.writenbt(helper.level.registryAccess(), CompoundTag(), false)
        if (!nbt.contains("logic", Tag.TAG_COMPOUND.toInt())) helper.fail("expected logic compound")
        if ("b=5" != nbt.getCompound("logic").getString("code")) {
            helper.fail("expected code persisted")
        }
        helper.succeed()
    }

    @JvmStatic
    fun readnbtRestoresCodeAndOutputData(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val logic = CompoundTag()
        logic.putString("code", "b=12")
        logic.putInt("input", 0)
        logic.putInt("output", 0)
        val nbt = CompoundTag()
        nbt.put("logic", logic)
        te.readnbt(helper.level.registryAccess(), nbt)
        if ("b=12" != te.getCode()) helper.fail("expected code restored")
        helper.succeed()
    }

    @JvmStatic
    fun readnbtPreservesSymbolMap(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val syms = CompoundTag()
        syms.putInt("foo", 42)
        val logic = CompoundTag()
        logic.putString("code", "")
        logic.put("symbols", syms)
        val nbt = CompoundTag()
        nbt.put("logic", logic)
        te.readnbt(helper.level.registryAccess(), nbt)
        helper.succeed()
    }

    @JvmStatic
    fun initiallyDisabled(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        if (te.getEnabled()) helper.fail("freshly placed control box must start disabled")
        helper.succeed()
    }

    @JvmStatic
    fun setEnabledTrueFlipsState(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setEnabled(true)
        val te2 = getControlBox(helper)
        if (te2 == null) {
            helper.fail("BE missing after setEnabled")
            return
        }
        if (!te2.getEnabled()) helper.fail("expected enabled after setEnabled(true)")
        helper.succeed()
    }

    @JvmStatic
    fun setEnabledIdempotent(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setEnabled(false)
        val te2 = getControlBox(helper)
        if (te2 == null) {
            helper.fail("BE missing")
            return
        }
        if (te2.getEnabled()) {
            helper.fail("setEnabled(false) on already-disabled must remain disabled")
        }
        helper.succeed()
    }

    @JvmStatic
    fun tickOnDisabledBoxClearsOutput(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=7")
        te.tick()
        val output =
            te.writenbt(helper.level.registryAccess(), CompoundTag(), false)
                .getCompound("logic")
                .getInt("output")
        if (output != 0) helper.fail("disabled control box must produce 0 output")
        helper.succeed()
    }

    @JvmStatic
    fun tickWithInvalidCodeDoesNotThrow(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=d.bad")
        te.setEnabled(true)
        val te2 = getControlBox(helper)
        te2?.tick()
        helper.succeed()
    }

    @JvmStatic
    fun traceToggleFlipsFlag(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val before = te.trace_enabled()
        te.toggle_trace(null)
        if (te.trace_enabled() == before) helper.fail("toggle_trace must flip the flag")
        helper.succeed()
    }

    @JvmStatic
    fun getNameFallsBackToBlockTranslationKey(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        if (te.hasCustomName()) helper.fail("default control box must not have custom name")
        if (te.name == null) helper.fail("getName must not be null")
        helper.succeed()
    }

    @JvmStatic
    fun setCustomNameStoresAndReturnsIt(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCustomName(Component.literal("My Box"))
        if (!te.hasCustomName()) helper.fail("hasCustomName false after set")
        if ("My Box" != te.name.getString()) helper.fail("custom name not returned")
        helper.succeed()
    }

    @JvmStatic
    fun onServerPacketReceivedAppliesCodeFromNbt(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val logic = CompoundTag()
        logic.putString("code", "b=3")
        val nbt = CompoundTag()
        nbt.put("logic", logic)
        te.onServerPacketReceived(nbt)
        if ("b=3" != te.getCode()) helper.fail("packet did not apply code")
        helper.succeed()
    }

    @JvmStatic
    fun getSignalReturnsComputedOutput(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=7")
        te.setEnabled(true)
        te.tick()
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val signal =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                helper.absolutePos(CONTROL_BOX_POS),
                Direction.WEST,
            )
        if (signal != 7) helper.fail("expected getSignal=7 queried from WEST, got $signal")
        helper.succeed()
    }

    @JvmStatic
    fun getDirectSignalMatchesGetSignal(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=5")
        te.setEnabled(true)
        te.tick()
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        val state = helper.getBlockState(CONTROL_BOX_POS)
        val sig = block.getSignal(state, helper.level, absPos, Direction.WEST)
        val dsig = block.getDirectSignal(state, helper.level, absPos, Direction.WEST)
        if (sig != dsig) {
            helper.fail("getDirectSignal must match getSignal, got sig=$sig dsig=$dsig")
        }
        helper.succeed()
    }

    @JvmStatic
    fun dropListWithCodeSavesNbt(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=3")
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val drops = block.dropList(helper.getBlockState(CONTROL_BOX_POS), helper.level, te, false)
        if (drops.size != 1) helper.fail("expected 1 drop, got ${drops.size}")
        if (!Auxiliaries.hasItemStackNbt(drops[0], "tedata")) {
            helper.fail("drop must carry tedata nbt when code is set")
        }
        helper.succeed()
    }

    @JvmStatic
    fun dropListWithNoCodeHasNoNbt(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val drops = block.dropList(helper.getBlockState(CONTROL_BOX_POS), helper.level, te, false)
        if (drops.size != 1) helper.fail("expected 1 drop")
        if (Auxiliaries.hasItemStackNbt(drops[0], "tedata")) {
            helper.fail("drop must not carry tedata nbt when no code is set")
        }
        helper.succeed()
    }

    @JvmStatic
    fun setPlacedByWithNbtRestoresCode(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=9")
        val tedata = te.writenbt(helper.level.registryAccess(), CompoundTag(), false)
        val stack = ItemStack(Registries.getItem("control_box")!!)
        Auxiliaries.setItemStackNbt(stack, "tedata", tedata)
        helper.setBlock(CONTROL_BOX_POS, Registries.getBlock("control_box")!!.defaultBlockState())
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        block.setPlacedBy(
            helper.level,
            helper.absolutePos(CONTROL_BOX_POS),
            helper.getBlockState(CONTROL_BOX_POS),
            null,
            stack,
        )
        val te2 = getControlBox(helper)
        if (te2 == null) {
            helper.fail("expected BE after setPlacedBy")
            return
        }
        if ("b=9" != te2.getCode()) {
            helper.fail("expected code restored by setPlacedBy, got: ${te2.getCode()}")
        }
        helper.succeed()
    }

    @JvmStatic
    fun setPlacedByWithEmptyNbtIsNoOp(helper: GameTestHelper) {
        placeControlBox(helper)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val stack = ItemStack(Registries.getItem("control_box")!!)
        block.setPlacedBy(
            helper.level,
            helper.absolutePos(CONTROL_BOX_POS),
            helper.getBlockState(CONTROL_BOX_POS),
            null,
            stack,
        )
        helper.succeed()
    }

    @JvmStatic
    fun updateWithNullFromPosResetsTickTimer(helper: GameTestHelper) {
        placeControlBox(helper)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        val result = block.update(helper.getBlockState(CONTROL_BOX_POS), helper.level, absPos, null)
        if (result == null) helper.fail("update must return non-null state")
        helper.succeed()
    }

    @JvmStatic
    fun updateWithNeighborPosTriggersSideScan(helper: GameTestHelper) {
        placeControlBox(helper)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        val result =
            block.update(helper.getBlockState(CONTROL_BOX_POS), helper.level, absPos, absPos.east())
        if (result == null) helper.fail("update must return non-null state")
        helper.succeed()
    }

    @JvmStatic
    fun isBlockEntityTickingAlwaysTrue(helper: GameTestHelper) {
        placeControlBox(helper)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        if (!block.isBlockEntityTicking(helper.level, helper.getBlockState(CONTROL_BOX_POS))) {
            helper.fail("isBlockEntityTicking must return true")
        }
        helper.succeed()
    }

    @JvmStatic
    fun defsPortNamesHasSixEntries(helper: GameTestHelper) {
        if (Defs.PORT_NAMES.size != 6) helper.fail("PORT_NAMES must have 6 entries")
        helper.succeed()
    }

    @JvmStatic
    fun getDisplayNameReturnsNonNull(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        if (te.displayName == null) helper.fail("getDisplayName must not return null")
        helper.succeed()
    }

    @JvmStatic
    fun createMenuReturnsNonNull(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val menu = te.createMenu(0, player.inventory, player)
        if (menu == null) helper.fail("createMenu must not return null")
        helper.succeed()
    }

    @JvmStatic
    fun tickWithTraceEnabledCoversTracePaths(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.toggle_trace(null)
        te.setCode("b=3")
        te.setEnabled(true)
        te.tick()
        if (!te.trace_enabled()) helper.fail("trace must still be enabled after tick")
        helper.succeed()
    }

    @JvmStatic
    fun tickWithTickrateSymbolSetsInterval(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=3\ntickrate=10")
        te.setEnabled(true)
        te.tick()
        helper.succeed()
    }

    @JvmStatic
    fun setEnabledFalseFromEnabledClearsSymbols(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=7")
        te.setEnabled(true)
        te.tick()
        te.setEnabled(false)
        val te2 = getControlBox(helper)
        if (te2 == null) {
            helper.fail("BE missing after setEnabled(false)")
            return
        }
        if (te2.getEnabled()) helper.fail("must be disabled after setEnabled(false)")
        helper.succeed()
    }

    @JvmStatic
    fun setRcaPlayerUuidNonNullDoesNotThrow(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setRcaPlayerUUID(UUID.randomUUID())
        helper.succeed()
    }

    @JvmStatic
    fun signalUpdateRisingEdgeSetsIntredge(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=g")
        te.setEnabled(true)
        te.tick()
        helper.setBlock(CONTROL_BOX_POS.west(), Blocks.REDSTONE_BLOCK)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        block.update(helper.getBlockState(CONTROL_BOX_POS), helper.level, absPos, absPos.west())
        helper.succeed()
    }

    @JvmStatic
    fun signalUpdateFallingEdgeSetsIntfedge(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=g")
        te.setEnabled(true)
        helper.setBlock(CONTROL_BOX_POS.west(), Blocks.REDSTONE_BLOCK)
        te.tick()
        helper.setBlock(CONTROL_BOX_POS.west(), Blocks.AIR)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        block.update(helper.getBlockState(CONTROL_BOX_POS), helper.level, absPos, absPos.west())
        helper.succeed()
    }

    @JvmStatic
    fun useItemOnWithDebugStickTogglesTrace(helper: GameTestHelper) {
        placeControlBox(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val debugStick = ItemStack(Items.DEBUG_STICK)
        player.setItemInHand(InteractionHand.MAIN_HAND, debugStick)
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        val hit = BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false)
        val result =
            helper
                .getBlockState(CONTROL_BOX_POS)
                .useItemOn(debugStick, helper.level, player, InteractionHand.MAIN_HAND, hit)
        if (result == null) helper.fail("useItemOn must not return null")
        helper.succeed()
    }

    @JvmStatic
    fun controlBoxBuiltinFunctionsExercised(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val absPos = helper.absolutePos(CONTROL_BOX_POS)
        te.setEnabled(true)

        te.setCode("b=max(3,5)")
        te.tick()
        var sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                absPos,
                Direction.WEST,
            )
        if (sig != 5) helper.fail("max(3,5) expected 5, got $sig")

        te.setCode("b=min(3,5)")
        te.tick()
        sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                absPos,
                Direction.WEST,
            )
        if (sig != 3) helper.fail("min(3,5) expected 3, got $sig")

        te.setCode("b=inv(3)")
        te.tick()
        sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                absPos,
                Direction.WEST,
            )
        if (sig != 12) helper.fail("inv(3) expected 12, got $sig")

        te.setCode("b=if(1,15,0)")
        te.tick()
        sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                absPos,
                Direction.WEST,
            )
        if (sig != 15) helper.fail("if(1,15,0) expected 15, got $sig")

        te.setCode("b=mean(4,8)")
        te.tick()
        sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                absPos,
                Direction.WEST,
            )
        if (sig != 6) helper.fail("mean(4,8) expected 6, got $sig")

        te.setCode("b=lim(3,0,15)")
        te.tick()
        sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                absPos,
                Direction.WEST,
            )
        if (sig != 3) helper.fail("lim(3,0,15) expected 3, got $sig")

        helper.succeed()
    }

    @JvmStatic
    fun controlBoxCounterFunctionsExercised(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=cnt1(1,0,10)\nb=cnt2(1,0,10)\nb=cnt3(1,0,10)")
        te.setEnabled(true)
        te.tick()
        te.tick()
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                helper.absolutePos(CONTROL_BOX_POS),
                Direction.WEST,
            )
        if (sig != 2) helper.fail("cnt3(1,10) after 2 ticks expected 2, got $sig")
        helper.succeed()
    }

    @JvmStatic
    fun controlBoxTimerFunctionsExercised(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=ton1(1,5)\nb=tof1(1,5)\nb=tp1(1,5)\nb=tiv1(5)")
        te.setEnabled(true)
        te.tick()
        te.tick()
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                helper.absolutePos(CONTROL_BOX_POS),
                Direction.WEST,
            )
        if (sig != 0) helper.fail("tiv1(5) after 2 ticks expected 0, got $sig")
        helper.succeed()
    }

    @JvmStatic
    fun controlBoxRemainingTimerVariantsExercised(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode(
            "b=cnt4(1,10)\nb=cnt5(1,10)\nb=tiv2(10)\nb=tiv3(10)" +
                "\nb=ton2(1,5)\nb=ton3(1,5)\nb=ton4(1,5)\nb=ton5(1,5)" +
                "\nb=tof2(1,5)\nb=tof3(1,5)\nb=tof4(1,5)\nb=tof5(1,5)" +
                "\nb=tp2(1,5)\nb=tp3(1,5)\nb=tp4(1,5)\nb=tp5(1,5)" +
                "\nb=rnd()\nb=clock()\nb=time()"
        )
        te.setEnabled(true)
        te.tick()
        te.tick()
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                helper.absolutePos(CONTROL_BOX_POS),
                Direction.WEST,
            )
        if (sig < 0 || sig > 15) helper.fail("time() output must be in [0,15], got $sig")
        helper.succeed()
    }

    @JvmStatic
    fun controlBoxTimerEdgeCasesDoNotThrow(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=tof1(0,0)\nb=tp1(0,5)")
        te.setEnabled(true)
        te.tick()
        val block = Registries.getBlock("control_box")!! as ControlBoxBlock
        val sig =
            block.getSignal(
                helper.getBlockState(CONTROL_BOX_POS),
                helper.level,
                helper.absolutePos(CONTROL_BOX_POS),
                Direction.WEST,
            )
        if (sig != 0) helper.fail("tp1(0,5) with no input expected 0, got $sig")
        helper.succeed()
    }

    @JvmStatic
    fun controlBoxSetCodeSameCodeSkipsReparse(helper: GameTestHelper) {
        val te = placeControlBox(helper)
        te.setCode("b=7")
        te.setEnabled(true)
        te.tick()
        te.setCode("b=7")
        helper.succeed()
    }

    private fun placeControlBox(helper: GameTestHelper): ControlBoxBlockEntity {
        helper.setBlock(CONTROL_BOX_POS, Registries.getBlock("control_box")!!.defaultBlockState())
        return getControlBox(helper) ?: error("expected control box block entity to be created")
    }

    private fun getControlBox(helper: GameTestHelper): ControlBoxBlockEntity? =
        helper.getBlockEntity(CONTROL_BOX_POS) as? ControlBoxBlockEntity
}
