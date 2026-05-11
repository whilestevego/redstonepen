@file:Suppress(
    "DEPRECATION"
) // CompoundTag.unsafe is deprecated in Java but has no replacement for direct NBT access

package wile.redstonepen.util

import com.mojang.blaze3d.platform.InputConstants
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.util.StringUtil
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import wile.redstonepen.ModConstants
import wile.redstonepen.platform.PlatformServices

object Auxiliaries {

    private val logger: Logger = org.slf4j.LoggerFactory.getLogger(ModConstants.MODID)
    private const val DEVELOPMENT_MODE_CONTROL_FILE = ".redstonepen-dev"
    private var developmentMode = false

    @JvmStatic
    fun init() {
        try {
            developmentMode =
                java.io
                    .File(getGameDirectory().resolve(DEVELOPMENT_MODE_CONTROL_FILE).toString())
                    .isFile()
        } catch (_: Throwable) {}
    }

    // -------------------------------------------------------------------------------------------------------------------
    // Mod specific exports
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic fun modid(): String = ModConstants.MODID

    @JvmStatic fun logger(): Logger = logger

    // -------------------------------------------------------------------------------------------------------------------
    // Sideness, system/environment, tagging interfaces
    // -------------------------------------------------------------------------------------------------------------------

    interface IExperimentalFeature

    @JvmStatic
    fun getGameDirectory(): java.nio.file.Path = PlatformServices.PLATFORM.getGameDirectory()

    @JvmStatic
    fun isModLoaded(registryName: String): Boolean =
        PlatformServices.PLATFORM.isModLoaded(registryName)

    @JvmStatic fun isDevelopmentMode(): Boolean = developmentMode

    @JvmStatic
    @Environment(EnvType.CLIENT)
    @Suppress("all")
    fun isShiftDown(): Boolean =
        InputConstants.isKeyDown(Minecraft.getInstance().window.window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(
                Minecraft.getInstance().window.window,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
            )

    @JvmStatic
    @Environment(EnvType.CLIENT)
    @Suppress("all")
    fun isCtrlDown(): Boolean =
        InputConstants.isKeyDown(
            Minecraft.getInstance().window.window,
            GLFW.GLFW_KEY_LEFT_CONTROL,
        ) ||
            InputConstants.isKeyDown(
                Minecraft.getInstance().window.window,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
            )

    @JvmStatic
    @Environment(EnvType.CLIENT)
    fun getClipboard(): Optional<String> =
        Optional.of(
            net.minecraft.client.gui.font.TextFieldHelper.getClipboardContents(
                Minecraft.getInstance()
            )
        )

    @JvmStatic
    @Environment(EnvType.CLIENT)
    fun setClipboard(text: String): Boolean {
        net.minecraft.client.gui.font.TextFieldHelper.setClipboardContents(
            Minecraft.getInstance(),
            text,
        )
        return true
    }

    // -------------------------------------------------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic fun logInfo(msg: String) = logger.info(msg)

    @JvmStatic fun logWarn(msg: String) = logger.warn(msg)

    @JvmStatic fun logError(msg: String) = logger.error(msg)

    // -------------------------------------------------------------------------------------------------------------------
    // Localization, text formatting
    // -------------------------------------------------------------------------------------------------------------------

    /**
     * Text localization wrapper, implicitly prepends `MODID` to the translation keys. Forces
     * formatting argument, nullable if no special formatting shall be applied.
     */
    @JvmStatic
    fun localizable(modtrkey: String, vararg args: Any): MutableComponent =
        Component.translatable(
            if (modtrkey.startsWith("block.") || modtrkey.startsWith("item.")) {
                modtrkey
            } else {
                "${modid()}.$modtrkey"
            },
            *args,
        )

    @JvmStatic
    fun localizable(modtrkey: String, color: ChatFormatting?, vararg args: Any): MutableComponent {
        val tr = Component.translatable("${modid()}.$modtrkey", *args)
        if (color != null) tr.withStyle(color)
        return tr
    }

    @JvmStatic
    fun localizable(modtrkey: String): MutableComponent = localizable(modtrkey, *emptyArray<Any>())

    @JvmStatic
    fun localizable_block_key(blocksubkey: String): MutableComponent =
        Component.translatable("block.${modid()}.$blocksubkey")

    @JvmStatic
    @Environment(EnvType.CLIENT)
    fun localize(translationKey: String, vararg args: Any): String =
        Component.translatable(translationKey, *args).string.trim()

    @JvmStatic
    @Environment(EnvType.CLIENT)
    fun wrapText(text: Component, maxWidthPercent: Int): List<Component> {
        val maxWidth = (Minecraft.getInstance().window.guiScaledWidth - 10) * maxWidthPercent / 100
        return Minecraft.getInstance()
            .font
            .splitter
            .splitLines(text, maxWidth, Style.EMPTY)
            .stream()
            .map { ft -> Component.literal(ft.string) }
            .collect(Collectors.toList())
    }

    @JvmStatic
    fun join(components: Collection<out Component>, separator: String): MutableComponent =
        ComponentUtils.formatList(components, Component.literal(separator), Function.identity())

    @JvmStatic
    fun join(vararg components: Component): MutableComponent {
        val tc = Component.empty()
        for (c in components) tc.append(c)
        return tc
    }

    @JvmStatic
    fun isEmpty(component: Component): Boolean =
        component.siblings.isEmpty() && component.string.isEmpty()

    object Tooltip {
        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun extendedTipCondition(): Boolean = isShiftDown() && !isCtrlDown()

        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun helpCondition(): Boolean = isShiftDown() && isCtrlDown()

        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun addInformation(
            advancedTooltipTranslationKey: String?,
            helpTranslationKey: String?,
            tooltip: MutableList<Component>,
            flag: TooltipFlag,
            addAdvancedTooltipHints: Boolean,
        ): Boolean {
            val helpAvailable =
                helpTranslationKey != null &&
                    net.minecraft.client.resources.language.I18n.exists("$helpTranslationKey.help")
            val tipAvailable =
                advancedTooltipTranslationKey != null &&
                    net.minecraft.client.resources.language.I18n.exists("$helpTranslationKey.tip")
            if (!helpAvailable && !tipAvailable) return false
            var tipText: MutableComponent = Component.empty()
            when {
                helpCondition() ->
                    if (helpAvailable) {
                        tipText = Component.literal(localize("$helpTranslationKey.help"))
                    }
                extendedTipCondition() ->
                    if (tipAvailable) {
                        tipText = Component.literal(localize("$advancedTooltipTranslationKey.tip"))
                    }
                addAdvancedTooltipHints -> {
                    if (tipAvailable) {
                        tipText =
                            Component.literal(
                                localize("${modid()}.tooltip.hint.extended") +
                                    if (helpAvailable) " " else ""
                            )
                    }
                    if (helpAvailable) {
                        tipText.append(Component.literal(localize("${modid()}.tooltip.hint.help")))
                    }
                }
            }
            if (isEmpty(tipText)) return false
            tooltip.addAll(wrapText(tipText, 50))
            return true
        }

        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun addInformation(
            stack: ItemStack,
            ctx: Item.TooltipContext,
            tooltip: MutableList<Component>,
            flag: TooltipFlag,
            addAdvancedTooltipHints: Boolean,
        ): Boolean =
            addInformation(
                stack.descriptionId,
                stack.descriptionId,
                tooltip,
                flag,
                addAdvancedTooltipHints,
            )
    }

    @JvmStatic
    fun playerChatMessage(player: Player, message: String) =
        player.displayClientMessage(Component.translatable(message.trim()), true)

    @JvmStatic
    fun unserializeTextComponent(serialized: String, ra: HolderLookup.Provider): Component? =
        Component.Serializer.fromJson(serialized, ra)

    @JvmStatic
    fun serializeTextComponent(tc: Component?, ra: HolderLookup.Provider?): String =
        if (tc == null) "" else Component.Serializer.toJson(tc, ra)

    // -------------------------------------------------------------------------------------------------------------------
    // Tag Handling
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic
    fun getResourceLocation(item: Item): ResourceLocation = BuiltInRegistries.ITEM.getKey(item)

    @JvmStatic
    fun getResourceLocation(block: Block): ResourceLocation = BuiltInRegistries.BLOCK.getKey(block)

    // -------------------------------------------------------------------------------------------------------------------
    // Item NBT data
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic
    fun hasItemStackNbt(stack: ItemStack, key: String): Boolean {
        val nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).unsafe
        return nbt != null && nbt.contains(key, CompoundTag.TAG_COMPOUND.toInt())
    }

    /**
     * Returns a *copy* of the custom data compound NBT entry selected via `key`, or an empty
     * CompoundTag if not existing.
     */
    @JvmStatic
    fun getItemStackNbt(stack: ItemStack, key: String): CompoundTag {
        val nbt =
            stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).unsafe
                ?: return CompoundTag()
        val data: Tag? = nbt.get(key)
        if (data == null || data.id.toInt() != CompoundTag.TAG_COMPOUND.toInt()) {
            return CompoundTag()
        }
        return data.copy() as CompoundTag
    }

    @JvmStatic
    fun setItemStackNbt(stack: ItemStack, key: String, nbt: CompoundTag?) {
        if (key.isEmpty()) return
        val cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag()))
        val cdt = cd.copyTag()
        if (nbt == null || nbt.isEmpty) cdt.remove(key) else cdt.put(key, nbt)
        CustomData.set(DataComponents.CUSTOM_DATA, stack, cdt)
    }

    /** Equivalent to getDisplayName(), returns null if no custom name is set. */
    @JvmStatic
    fun getItemLabel(stack: ItemStack): Component? =
        stack.components.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty())

    @JvmStatic
    fun setItemLabel(stack: ItemStack, name: Component?): ItemStack {
        if (name == null || StringUtil.isBlank(name.string)) {
            if (stack.has(DataComponents.CUSTOM_NAME)) stack.remove(DataComponents.CUSTOM_NAME)
        } else {
            stack.set(DataComponents.CUSTOM_NAME, name.copy())
        }
        return stack
    }

    // -------------------------------------------------------------------------------------------------------------------
    // Block handling
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic
    fun isWaterLogged(state: BlockState): Boolean =
        state.hasProperty(BlockStateProperties.WATERLOGGED) &&
            state.getValue(BlockStateProperties.WATERLOGGED)

    @JvmStatic
    fun getPixeledAABB(
        x0: Double,
        y0: Double,
        z0: Double,
        x1: Double,
        y1: Double,
        z1: Double,
    ): AABB = AABB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0)

    @JvmStatic
    fun getRotatedAABB(bb: AABB, newFacing: Direction): AABB = getRotatedAABB(bb, newFacing, false)

    @JvmStatic
    fun getRotatedAABB(bb: Array<AABB>, newFacing: Direction): Array<AABB> =
        getRotatedAABB(bb, newFacing, false)

    @JvmStatic
    fun getRotatedAABB(bb: AABB, newFacing: Direction, horizontalRotation: Boolean): AABB {
        if (!horizontalRotation) {
            return when (newFacing.get3DDataValue()) {
                0 -> AABB(1 - bb.maxX, bb.minZ, bb.minY, 1 - bb.minX, bb.maxZ, bb.maxY) // D
                1 ->
                    AABB(
                        1 - bb.maxX,
                        1 - bb.maxZ,
                        1 - bb.maxY,
                        1 - bb.minX,
                        1 - bb.minZ,
                        1 - bb.minY,
                    ) // U
                2 -> AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ) // N --> bb
                3 -> AABB(1 - bb.maxX, bb.minY, 1 - bb.maxZ, 1 - bb.minX, bb.maxY, 1 - bb.minZ) // S
                4 -> AABB(bb.minZ, bb.minY, 1 - bb.maxX, bb.maxZ, bb.maxY, 1 - bb.minX) // W
                5 -> AABB(1 - bb.maxZ, bb.minY, bb.minX, 1 - bb.minZ, bb.maxY, bb.maxX) // E
                else -> bb
            }
        }
        return when (newFacing.get3DDataValue()) {
            0 -> AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ) // D --> bb
            1 -> AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ) // U --> bb
            2 -> AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ) // N --> bb
            3 -> AABB(1 - bb.maxX, bb.minY, 1 - bb.maxZ, 1 - bb.minX, bb.maxY, 1 - bb.minZ) // S
            4 -> AABB(bb.minZ, bb.minY, 1 - bb.maxX, bb.maxZ, bb.maxY, 1 - bb.minX) // W
            5 -> AABB(1 - bb.maxZ, bb.minY, bb.minX, 1 - bb.minZ, bb.maxY, bb.maxX) // E
            else -> bb
        }
    }

    @JvmStatic
    fun getRotatedAABB(
        bbs: Array<AABB>,
        newFacing: Direction,
        horizontalRotation: Boolean,
    ): Array<AABB> = Array(bbs.size) { i -> getRotatedAABB(bbs[i], newFacing, horizontalRotation) }

    @JvmStatic
    fun getYRotatedAABB(bb: AABB, clockwise90degSteps: Int): AABB {
        val directionMap = arrayOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
        return getRotatedAABB(bb, directionMap[(clockwise90degSteps + 4096) and 0x03], true)
    }

    @JvmStatic
    fun getYRotatedAABB(bbs: Array<AABB>, clockwise90degSteps: Int): Array<AABB> =
        Array(bbs.size) { i -> getYRotatedAABB(bbs[i], clockwise90degSteps) }

    @JvmStatic
    fun getMirroredAABB(bb: AABB, axis: Direction.Axis): AABB =
        when (axis) {
            Direction.Axis.X -> AABB(1 - bb.maxX, bb.minY, bb.minZ, 1 - bb.minX, bb.maxY, bb.maxZ)
            Direction.Axis.Y -> AABB(bb.minX, 1 - bb.maxY, bb.minZ, bb.maxX, 1 - bb.minY, bb.maxZ)
            Direction.Axis.Z -> AABB(bb.minX, bb.minY, 1 - bb.maxZ, bb.maxX, bb.maxY, 1 - bb.minZ)
        }

    @JvmStatic
    fun getMirroredAABB(bbs: Array<AABB>, axis: Direction.Axis): Array<AABB> =
        Array(bbs.size) { i -> getMirroredAABB(bbs[i], axis) }

    @JvmStatic
    fun getUnionShape(vararg aabbs: AABB): VoxelShape {
        var shape = Shapes.empty()
        for (aabb in aabbs) shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR)
        return shape
    }

    @JvmStatic
    fun getUnionShape(vararg aabbList: Array<AABB>): VoxelShape {
        var shape = Shapes.empty()
        for (aabbs in aabbList) {
            for (aabb in aabbs) {
                shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR)
            }
        }
        return shape
    }

    @JvmStatic
    fun getMappedAABB(bbs: Array<AABB>, mapper: Function<AABB, AABB>): Array<AABB> =
        Array(bbs.size) { i -> mapper.apply(bbs[i]) }

    class BlockPosRange(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int) : Iterable<BlockPos> {
        internal val x0: Int
        internal val x1: Int
        internal val y0: Int
        internal val y1: Int
        internal val z0: Int
        internal val z1: Int

        init {
            this.x0 = minOf(x0, x1)
            this.x1 = maxOf(x0, x1)
            this.y0 = minOf(y0, y1)
            this.y1 = maxOf(y0, y1)
            this.z0 = minOf(z0, z1)
            this.z1 = maxOf(z0, z1)
        }

        companion object {
            @JvmStatic
            fun of(range: AABB): BlockPosRange =
                BlockPosRange(
                    Math.floor(range.minX).toInt(),
                    Math.floor(range.minY).toInt(),
                    Math.floor(range.minZ).toInt(),
                    Math.floor(range.maxX - 0.0625).toInt(),
                    Math.floor(range.maxY - 0.0625).toInt(),
                    Math.floor(range.maxZ - 0.0625).toInt(),
                )
        }

        fun getXSize(): Int = x1 - x0 + 1

        fun getYSize(): Int = y1 - y0 + 1

        fun getZSize(): Int = z1 - z0 + 1

        fun getArea(): Int = getXSize() * getZSize()

        fun getHeight(): Int = getYSize()

        fun getVolume(): Int = getXSize() * getYSize() * getZSize()

        fun byXZYIndex(xyzIndex: Int): BlockPos {
            val xsz = getXSize()
            val ysz = getYSize()
            val zsz = getZSize()
            var idx = xyzIndex % (xsz * ysz * zsz)
            val y = idx / (xsz * zsz)
            idx -= y * (xsz * zsz)
            val z = idx / xsz
            idx -= z * xsz
            return BlockPos(x0 + idx, y0 + y, z0 + z)
        }

        fun byXZIndex(xzIndex: Int, yOffset: Int): BlockPos {
            val xsz = getXSize()
            val zsz = getZSize()
            var idx = xzIndex % (xsz * zsz)
            val z = idx / xsz
            idx -= z * xsz
            return BlockPos(x0 + idx, y0 + yOffset, z0 + z)
        }

        class BlockRangeIterator(private val range: BlockPosRange) : Iterator<BlockPos> {
            private var x = range.x0
            private var y = range.y0
            private var z = range.z0

            override fun hasNext(): Boolean = z <= range.z1

            override fun next(): BlockPos {
                if (!hasNext()) throw NoSuchElementException()
                val pos = BlockPos(x, y, z)
                if (++x > range.x1) {
                    x = range.x0
                    if (++y > range.y1) {
                        y = range.y0
                        ++z
                    }
                }
                return pos
            }
        }

        override fun iterator(): BlockRangeIterator = BlockRangeIterator(this)

        fun stream(): java.util.stream.Stream<BlockPos> =
            java.util.stream.StreamSupport.stream(spliterator(), false)
    }

    // -------------------------------------------------------------------------------------------------------------------
    // JAR resource related
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic
    fun loadResourceText(stream: InputStream?): String =
        try {
            if (stream == null) {
                ""
            } else {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"))
            }
        } catch (_: Throwable) {
            ""
        }

    @JvmStatic
    fun loadResourceText(path: String): String =
        loadResourceText(Auxiliaries::class.java.getResourceAsStream(path))

    @JvmStatic
    fun logGitVersion() {
        try {
            val version = loadResourceText("/.gitversion-${ModConstants.MODID}").trim()
            logInfo(
                ModConstants.MODNAME +
                    (if (version.isEmpty()) " (dev build)" else " GIT id #$version") +
                    "."
            )
        } catch (_: Throwable) {}
    }

    // -------------------------------------------------------------------------------------------------------------------
    // Particle spawning
    // -------------------------------------------------------------------------------------------------------------------

    @JvmStatic
    fun particles(world: Level, pos: BlockPos, type: ParticleOptions) =
        particles(world, Vec3.atCenterOf(pos).add(0.0, 0.4, 0.0), type, 1f)

    @JvmStatic
    fun particles(world: Level, pos: Vec3, type: ParticleOptions, velocity: Float) {
        val rand: RandomSource = world.random
        val sl = world as? ServerLevel ?: return
        sl.sendParticles(
            type,
            pos.x + rand.nextGaussian() * 0.2,
            pos.y + rand.nextGaussian() * 0.2,
            pos.z + rand.nextGaussian() * 0.2,
            1,
            rand.nextDouble() * 2e-2,
            rand.nextDouble() * 2e-2,
            rand.nextDouble() * 2e-2,
            velocity * 0.1,
        )
    }

    @JvmStatic
    fun getFakePlayer(world: Level): Optional<out Player> =
        PlatformServices.PLATFORM.getFakePlayer(world as ServerLevel)
}
