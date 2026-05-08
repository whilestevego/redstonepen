/*
 * @file RsSignals.kt
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General redstone signal related functionality.
 */
package wile.redstonepen.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

object RsSignals {

    @JvmStatic
    fun hasSignalConnector(state: BlockState, world: BlockGetter, pos: BlockPos, realSide: Direction?): Boolean =
        state.isSignalSource

    @JvmStatic
    fun fromContainer(container: Container?): Int {
        if (container == null) return 0
        val max = container.maxStackSize.toDouble()
        if (max <= 0) return 0
        var nonempty = false
        var fillLevel = 0.0
        for (i in 0 until container.containerSize) {
            val stack: ItemStack = container.getItem(i)
            if (stack.isEmpty || stack.maxStackSize <= 0) continue
            fillLevel += stack.count.toDouble() / minOf(max, stack.maxStackSize.toDouble())
            nonempty = true
        }
        fillLevel /= container.containerSize
        return (Math.floor(fillLevel * 14) + if (nonempty) 1 else 0).toInt() // vanilla compliant calculation.
    }

    @JvmStatic
    fun canEmitWeakPower(state: BlockState, world: Level, pos: BlockPos, redstoneSide: Direction): Boolean =
        state.isRedstoneConductor(world, pos)
}
