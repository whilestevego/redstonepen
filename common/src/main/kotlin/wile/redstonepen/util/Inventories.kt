package wile.redstonepen.util

import java.util.*
import java.util.function.BiFunction
import java.util.function.BiPredicate
import java.util.stream.Stream
import java.util.stream.StreamSupport
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

object Inventories {

    @JvmStatic
    fun areItemStacksIdentical(a: ItemStack, b: ItemStack): Boolean =
        a.item === b.item && ItemStack.isSameItemSameComponents(a, b)

    @JvmStatic
    fun areItemStacksDifferent(a: ItemStack, b: ItemStack): Boolean =
        a.item !== b.item || !ItemStack.isSameItemSameComponents(a, b)

    @JvmStatic
    fun areItemStacksIdenticalIgnoreDamage(a: ItemStack, b: ItemStack): Boolean {
        if (a.item !== b.item) return false
        if (!a.isDamageableItem) return ItemStack.isSameItemSameComponents(a, b)
        val bc: DataComponentMap = b.components
        return a.components.stream().allMatch { aTdc ->
            if (!bc.has(aTdc.type())) return@allMatch false
            if (aTdc.value() == bc.get(aTdc.type())) return@allMatch true
            aTdc.type() == DataComponents.DAMAGE
        }
    }

    @JvmStatic
    fun isItemStackableOn(a: ItemStack, b: ItemStack): Boolean =
        !a.isEmpty && a.isStackable && ItemStack.isSameItem(a, b)

    @JvmStatic
    fun extract(player: Player, match: ItemStack?, amount: Int, simulate: Boolean): ItemStack {
        if (amount <= 0) return ItemStack.EMPTY
        val ir = InventoryRange.fromPlayerInventory(player)
        return if (match == null) {
            ir.extract(amount, false, simulate)
        } else {
            val mstack = match.copy()
            mstack.setCount(amount)
            ir.extract(mstack, simulate)
        }
    }

    @JvmStatic
    fun insert(player: Player, stack: ItemStack, simulate: Boolean): ItemStack =
        InventoryRange.fromPlayerInventory(player).insert(stack, simulate)

    @JvmStatic
    fun copyOf(src: Container): Container {
        val size = src.containerSize
        val dst = SimpleContainer(size)
        for (i in 0 until size) dst.setItem(i, src.getItem(i).copy())
        return dst
    }

    @JvmStatic
    fun insert(toRanges: Array<InventoryRange>, stack: ItemStack): ItemStack {
        var remaining = stack.copy()
        for (range in toRanges) {
            remaining =
                range.insert(
                    remaining,
                    onlyFillup = false,
                    limit = 0,
                    reverse = false,
                    forceGroupStacks = true,
                )
            if (remaining.isEmpty) return remaining
        }
        return remaining
    }

    @JvmStatic
    fun give(entity: Player, stack: ItemStack) {
        entity.inventory.placeItemBackInInventory(stack)
    }

    // ------------------------------------------------------------------------------------------------------------------

    open class InventoryRange : Container, Iterable<ItemStack> {
        protected val inventory: Container
        protected val offset: Int
        protected val size: Int
        protected val numRows: Int
        private var maxStackSize_: Int = 64
        private var validator_: BiPredicate<Int, ItemStack> = BiPredicate { _, _ -> true }

        constructor(inventory: Container, offset: Int, size: Int, numRows: Int) {
            this.inventory = inventory
            this.offset = Mth.clamp(offset, 0, inventory.containerSize - 1)
            this.size = Mth.clamp(size, 0, inventory.containerSize - this.offset)
            this.numRows = numRows
        }

        constructor(inventory: Container, offset: Int, size: Int) : this(inventory, offset, size, 1)

        constructor(inventory: Container) : this(inventory, 0, inventory.containerSize, 1)

        companion object {
            @JvmStatic
            fun fromPlayerHotbar(player: Player): InventoryRange =
                InventoryRange(player.inventory, 0, 9, 1)

            @JvmStatic
            fun fromPlayerStorage(player: Player): InventoryRange =
                InventoryRange(player.inventory, 9, 27, 3)

            @JvmStatic
            fun fromPlayerInventory(player: Player): InventoryRange =
                InventoryRange(player.inventory, 0, 36, 4)
        }

        fun inventory(): Container = inventory

        fun size(): Int = size

        fun offset(): Int = offset

        fun get(index: Int): ItemStack = inventory.getItem(offset + index)

        fun set(index: Int, stack: ItemStack) = inventory.setItem(offset + index, stack)

        fun setValidator(validator: BiPredicate<Int, ItemStack>): InventoryRange {
            validator_ = validator
            return this
        }

        fun getValidator(): BiPredicate<Int, ItemStack> = validator_

        fun setMaxStackSize(count: Int): InventoryRange {
            maxStackSize_ = maxOf(count, 1)
            return this
        }

        // Container
        // --------------------------------------------------------------------------------------------------------

        override fun clearContent() {
            for (i in 0 until size) setItem(i, ItemStack.EMPTY)
        }

        override fun getContainerSize(): Int = size

        override fun isEmpty(): Boolean {
            for (i in 0 until size) if (!inventory.getItem(offset + i).isEmpty) return false
            return true
        }

        override fun getItem(index: Int): ItemStack = inventory.getItem(offset + index)

        override fun removeItem(index: Int, count: Int): ItemStack =
            inventory.removeItem(offset + index, count)

        override fun removeItemNoUpdate(index: Int): ItemStack =
            inventory.removeItemNoUpdate(offset + index)

        override fun setItem(index: Int, stack: ItemStack) =
            inventory.setItem(offset + index, stack)

        override fun getMaxStackSize(): Int = minOf(maxStackSize_, inventory.maxStackSize)

        override fun setChanged() = inventory.setChanged()

        override fun stillValid(player: Player): Boolean = inventory.stillValid(player)

        override fun startOpen(player: Player) = inventory.startOpen(player)

        override fun stopOpen(player: Player) = inventory.stopOpen(player)

        override fun canPlaceItem(index: Int, stack: ItemStack): Boolean =
            validator_.test(offset + index, stack) && inventory.canPlaceItem(offset + index, stack)

        // ------------------------------------------------------------------------------------------------------------------

        /**
         * Iterates using a function (slot, stack) -> bool until the function matches (returns
         * true).
         */
        fun iterate(fn: BiPredicate<Int, ItemStack>): Boolean {
            for (i in 0 until size) if (fn.test(i, getItem(i))) return true
            return false
        }

        fun contains(stack: ItemStack): Boolean {
            for (i in 0 until size) if (areItemStacksIdentical(stack, getItem(i))) return true
            return false
        }

        fun indexOf(stack: ItemStack): Int {
            for (i in 0 until size) if (areItemStacksIdentical(stack, getItem(i))) return i
            return -1
        }

        fun <T : Any> find(fn: BiFunction<Int, ItemStack, Optional<T>>): Optional<T> {
            for (i in 0 until size) {
                val r = fn.apply(i, getItem(i))
                if (r.isPresent) return r
            }
            return Optional.empty()
        }

        fun <T : Any> collect(fn: BiFunction<Int, ItemStack, Optional<T>>): List<T> {
            val data = mutableListOf<T>()
            for (i in 0 until size) fn.apply(i, getItem(i)).ifPresent { data.add(it) }
            return data
        }

        fun stream(): Stream<ItemStack> = StreamSupport.stream(spliterator(), false)

        override fun iterator(): Iterator<ItemStack> = InventoryRangeIterator(this)

        // ------------------------------------------------------------------------------------------------------------------

        /** Returns the number of stacks that match the given stack with NBT. */
        fun stackMatchCount(refStack: ItemStack): Int {
            var n = 0
            for (i in 0 until size) if (areItemStacksIdentical(refStack, getItem(i))) ++n
            return n
        }

        fun totalMatchingItemCount(refStack: ItemStack): Int {
            var n = 0
            for (i in 0 until size) {
                val stack = getItem(i)
                if (areItemStacksIdentical(refStack, stack)) n += stack.count
            }
            return n
        }

        // ------------------------------------------------------------------------------------------------------------------

        /**
         * Moves as much items from the stack to the slots in range [offset, end_slot] of the
         * inventory, filling up existing stacks first, then (player inventory only) checks
         * appropriate empty slots next to stacks that have that item already, and last uses any
         * empty slot that can be found. Returns the stack that is still remaining in the referenced
         * `stack`.
         */
        fun insert(
            inputStack: ItemStack,
            onlyFillup: Boolean,
            limit: Int,
            reverse: Boolean,
            forceGroupStacks: Boolean,
        ): ItemStack {
            val mvstack = inputStack.copy()
            if (mvstack.isEmpty) return checked(mvstack)
            var limitLeft =
                if (limit > 0) minOf(limit, mvstack.maxStackSize) else mvstack.maxStackSize
            val matches = BooleanArray(size)
            val empties = BooleanArray(size)
            var numMatches = 0
            for (i in 0 until size) {
                val sno = if (reverse) size - 1 - i else i
                val stack = getItem(sno)
                if (stack.isEmpty) {
                    empties[sno] = true
                } else if (areItemStacksIdentical(stack, mvstack)) {
                    matches[sno] = true
                    ++numMatches
                }
            }
            // first iteration: fillup existing stacks
            for (i in 0 until size) {
                val sno = if (reverse) size - 1 - i else i
                if (empties[sno] || !matches[sno]) continue
                val stack = getItem(sno)
                val nmax = minOf(limitLeft, stack.maxStackSize - stack.count)
                if (mvstack.count <= nmax) {
                    stack.setCount(stack.count + mvstack.count)
                    setItem(sno, stack)
                    return ItemStack.EMPTY
                } else {
                    mvstack.shrink(nmax)
                    limitLeft -= nmax
                    stack.grow(nmax)
                    setItem(sno, stack)
                }
            }
            if (onlyFillup) return checked(mvstack)
            if (numMatches > 0 && (forceGroupStacks || inventory is Inventory)) {
                // second iteration: use appropriate empty slots,
                // a) between
                run {
                    var insertStart = -1
                    var insertEnd = -1
                    var i = 1
                    while (i < size - 1) {
                        val sno = if (reverse) size - 1 - i else i
                        if (insertStart < 0) {
                            if (matches[sno]) insertStart = sno
                        } else if (matches[sno]) {
                            insertEnd = sno
                        }
                        i++
                    }
                    i = insertStart
                    while (i < insertEnd) {
                        val sno = if (reverse) size - 1 - i else i
                        if (empties[sno] && canPlaceItem(sno, mvstack)) {
                            val nmax = minOf(limitLeft, mvstack.count)
                            val moved = mvstack.copy()
                            moved.setCount(nmax)
                            mvstack.shrink(nmax)
                            setItem(sno, moved)
                            return checked(mvstack)
                        }
                        i++
                    }
                }
                // b) before/after
                for (i in 1 until size - 1) {
                    val sno = if (reverse) size - 1 - i else i
                    if (!matches[sno]) continue
                    val ii =
                        if (empties[sno - 1]) {
                            sno - 1
                        } else if (empties[sno + 1]) {
                            sno + 1
                        } else {
                            -1
                        }
                    if (ii >= 0 && canPlaceItem(ii, mvstack)) {
                        val nmax = minOf(limitLeft, mvstack.count)
                        val moved = mvstack.copy()
                        moved.setCount(nmax)
                        mvstack.shrink(nmax)
                        setItem(ii, moved)
                        return checked(mvstack)
                    }
                }
            }
            // third iteration: use any empty slots
            for (i in 0 until size) {
                val sno = if (reverse) size - 1 - i else i
                if (!empties[sno] || !canPlaceItem(sno, mvstack)) continue
                val nmax = minOf(limitLeft, mvstack.count)
                val placed = mvstack.copy()
                placed.setCount(nmax)
                mvstack.shrink(nmax)
                setItem(sno, placed)
                return checked(mvstack)
            }
            return checked(mvstack)
        }

        fun insert(inputStack: ItemStack, simulate: Boolean): ItemStack {
            if (inputStack.isEmpty) return ItemStack.EMPTY
            if (!simulate) return insert(inputStack)
            val stack = inputStack.copy()
            for (s in this) {
                if (s.isEmpty) return ItemStack.EMPTY
                val nleft = s.count - s.maxStackSize
                if (nleft <= 0 || !isItemStackableOn(s, stack)) continue
                if (nleft >= stack.count) return ItemStack.EMPTY
                stack.shrink(nleft)
            }
            return stack
        }

        fun insert(stackToMove: ItemStack): ItemStack =
            insert(
                stackToMove,
                onlyFillup = false,
                limit = 0,
                reverse = false,
                forceGroupStacks = true,
            )

        fun insert(index: Int, stackToMove: ItemStack): ItemStack {
            if (stackToMove.isEmpty) return stackToMove
            val stack = getItem(index)
            val limit = minOf(maxStackSize, stack.maxStackSize)
            return when {
                stack.isEmpty -> {
                    setItem(index, stackToMove.copy())
                    ItemStack.EMPTY
                }
                stack.count >= limit || !areItemStacksIdentical(stack, stackToMove) -> stackToMove
                else -> {
                    val amount = minOf(limit - stack.count, stackToMove.count)
                    val remaining = stackToMove.copy()
                    remaining.shrink(amount)
                    stack.grow(amount)
                    if (remaining.isEmpty) ItemStack.EMPTY else remaining
                }
            }
        }

        // ------------------------------------------------------------------------------------------------------------------

        /**
         * Extracts maximum amount of items from the inventory. The first non-empty stack defines
         * the item.
         */
        fun extract(amount: Int): ItemStack = extract(amount, false)

        fun extract(amount: Int, _random: Boolean): ItemStack =
            extract(amount, random = false, simulate = false)

        fun extract(amount: Int, random: Boolean, simulate: Boolean): ItemStack {
            var outStack = ItemStack.EMPTY
            var remaining = amount
            val offset = if (random) (Math.random() * size).toInt() else 0
            for (k in 0 until size) {
                val i = (offset + k) % size
                val stack = getItem(i)
                if (stack.isEmpty) continue
                if (outStack.isEmpty) {
                    if (stack.count < remaining) {
                        outStack = stack
                        if (!simulate) setItem(i, ItemStack.EMPTY)
                        if (!outStack.isStackable) break
                        remaining -= outStack.count
                    } else {
                        outStack =
                            if (!simulate) {
                                stack.split(remaining)
                            } else {
                                stack.copy().also { it.setCount(remaining) }
                            }
                        break
                    }
                } else if (areItemStacksIdentical(stack, outStack)) {
                    if (stack.count <= remaining) {
                        outStack.grow(stack.count)
                        remaining -= stack.count
                        if (!simulate) setItem(i, ItemStack.EMPTY)
                    } else {
                        outStack.grow(remaining)
                        if (!simulate) {
                            stack.shrink(remaining)
                            if (stack.isEmpty) setItem(i, ItemStack.EMPTY)
                        }
                        break
                    }
                }
            }
            if (!outStack.isEmpty && !simulate) setChanged()
            return outStack
        }

        fun extract(requestStack: ItemStack): ItemStack = extract(requestStack, false)

        fun extract(requestStack: ItemStack, simulate: Boolean): ItemStack {
            if (requestStack.isEmpty) return ItemStack.EMPTY
            val matchList = mutableListOf<ItemStack>()
            for (i in 0 until size) {
                val stack = getItem(i)
                if (!stack.isEmpty && areItemStacksIdenticalIgnoreDamage(stack, requestStack)) {
                    matchList.add(stack)
                }
            }
            matchList.sortWith(Comparator.comparingInt { it.count })
            if (matchList.isEmpty()) return ItemStack.EMPTY
            if (!simulate) {
                var nLeft = requestStack.count
                val fetched = matchList[0].split(nLeft)
                nLeft -= fetched.count
                for (i in 1 until matchList.size) {
                    if (nLeft <= 0) break
                    val s = matchList[i].split(nLeft)
                    nLeft -= s.count
                    fetched.grow(s.count)
                }
                return checked(fetched)
            } else {
                var amount = 0
                for (m in matchList) amount += m.count
                if (amount == 0) return ItemStack.EMPTY
                val stack = requestStack.copy()
                if (amount < stack.count) stack.setCount(amount)
                return stack
            }
        }

        // ------------------------------------------------------------------------------------------------------------------

        /**
         * Moves items from this inventory range to another. Returns true if something was moved (if
         * the inventories should be marked dirty).
         */
        fun move(
            index: Int,
            targetRange: InventoryRange,
            allIdenticalStacks: Boolean,
            onlyFillup: Boolean,
            reverse: Boolean,
            forceGroupStacks: Boolean,
        ): Boolean {
            val sourceStack = getItem(index)
            if (sourceStack.isEmpty) return false
            if (!allIdenticalStacks) {
                val remaining =
                    targetRange.insert(sourceStack, onlyFillup, 0, reverse, forceGroupStacks)
                setItem(index, remaining)
                return remaining.count != sourceStack.count
            } else {
                var remaining = sourceStack.copy()
                setItem(index, ItemStack.EMPTY)
                val refStack = remaining.copy()
                refStack.setCount(refStack.maxStackSize)
                var i = size
                while (i > 0 && !remaining.isEmpty) {
                    remaining =
                        targetRange.insert(remaining, onlyFillup, 0, reverse, forceGroupStacks)
                    if (!remaining.isEmpty) break
                    remaining = this.extract(refStack)
                    i--
                }
                if (!remaining.isEmpty) setItem(index, remaining)
                return remaining.count != sourceStack.count
            }
        }

        fun move(index: Int, targetRange: InventoryRange): Boolean =
            move(
                index,
                targetRange,
                allIdenticalStacks = false,
                onlyFillup = false,
                reverse = false,
                forceGroupStacks = true,
            )

        /**
         * Moves/clears the complete range to another range if possible. Returns true if something
         * was moved (if the inventories should be marked dirty).
         */
        fun move(
            targetRange: InventoryRange,
            onlyFillup: Boolean,
            reverse: Boolean,
            forceGroupStacks: Boolean,
        ): Boolean {
            var changed = false
            for (i in 0 until size) {
                changed =
                    changed or move(i, targetRange, false, onlyFillup, reverse, forceGroupStacks)
            }
            return changed
        }

        fun move(targetRange: InventoryRange, onlyFillup: Boolean): Boolean =
            move(targetRange, onlyFillup, reverse = false, forceGroupStacks = true)

        fun move(targetRange: InventoryRange): Boolean =
            move(targetRange, onlyFillup = false, reverse = false, forceGroupStacks = true)

        private fun checked(stack: ItemStack): ItemStack =
            if (stack.isEmpty) ItemStack.EMPTY else stack
    }

    // ------------------------------------------------------------------------------------------------------------------

    class InventoryRangeIterator(private val parent: InventoryRange) : Iterator<ItemStack> {
        private var index = 0

        override fun hasNext(): Boolean = index < parent.size()

        override fun next(): ItemStack {
            if (index >= parent.size()) throw NoSuchElementException()
            return parent.getItem(index++)
        }
    }
}
