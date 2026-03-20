package cat.daisy.menu

import cat.daisy.menu.text.DaisyText.mm
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import kotlin.jvm.JvmName

/**
 * DSL builder for creating immutable [Menu] definitions.
 */
public class MenuBuilder(
    public var title: String = "",
    public var rows: Int = 3,
) {
    private val slots = mutableMapOf<Int, SlotDefinition>()
    private val openHandlers = mutableListOf<suspend (MenuSession) -> Unit>()
    private val closeHandlers = mutableListOf<suspend (MenuSession) -> Unit>()
    private var pagination: PaginationDefinition? = null

    internal val size: Int
        get() = rows * 9

    public fun slot(
        index: Int,
        button: Button,
    ) {
        setSlotDefinition(index, button.definition)
    }

    public fun slot(
        index: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        setSlotDefinition(index, SlotBuilder().apply(block).build())
    }

    public fun slot(
        x: Int,
        y: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(x in 1..9) { "Column must be between 1 and 9" }
        require(y in 1..rows) { "Row must be between 1 and $rows" }
        slot((x - 1) + ((y - 1) * 9), block)
    }

    public fun fill(block: ItemBuilder.() -> Unit = {}) {
        fill(Material.GRAY_STAINED_GLASS_PANE, block)
    }

    public fun fill(
        material: Material,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val definition = SlotBuilder().apply { item(material, block) }.build()
        repeat(size) { slot ->
            setSlotDefinition(slot, definition, overwrite = false)
        }
    }

    public fun fillSlots(
        vararg indices: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val definition = SlotBuilder().apply(block).build()
        indices.forEach { index ->
            setSlotDefinition(index, definition)
        }
    }

    public fun fillRow(
        row: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(row in 1..rows) { "Row must be between 1 and $rows" }
        val definition = SlotBuilder().apply(block).build()
        val start = (row - 1) * 9
        for (slot in start until start + 9) {
            setSlotDefinition(slot, definition)
        }
    }

    public fun fillColumn(
        column: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(column in 1..9) { "Column must be between 1 and 9" }
        val definition = SlotBuilder().apply(block).build()
        for (row in 0 until rows) {
            setSlotDefinition((row * 9) + (column - 1), definition)
        }
    }

    public fun fillBorder(
        material: Material = Material.GRAY_STAINED_GLASS_PANE,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val definition = SlotBuilder().apply { item(material, block) }.build()
        topRowSlots().forEach { slot -> setSlotDefinition(slot, definition) }
        bottomRowSlots().forEach { slot -> setSlotDefinition(slot, definition) }
        for (row in 1 until rows - 1) {
            setSlotDefinition(row * 9, definition)
            setSlotDefinition((row * 9) + 8, definition)
        }
    }

    public fun pattern(
        vararg lines: String,
        mapping: PatternMapping.() -> Unit,
    ) {
        require(lines.size <= rows) { "Pattern has ${lines.size} rows but menu only has $rows rows" }
        lines.forEach { line ->
            require(line.length <= 9) { "Pattern lines must not be wider than 9 columns" }
        }

        val patternMapping = PatternMapping().apply(mapping)
        lines.forEachIndexed { rowIndex, line ->
            line.forEachIndexed { column, character ->
                if (character == ' ') {
                    return@forEachIndexed
                }
                val block = patternMapping.getBuilder(character) ?: return@forEachIndexed
                slot((rowIndex * 9) + column, block)
            }
        }
    }

    public fun pagination(
        itemsPerPage: Int,
        block: suspend PaginationScope.() -> Unit,
    ) {
        require(itemsPerPage > 0) { "Items per page must be greater than 0" }
        pagination = PaginationDefinition(itemsPerPage, block)
    }

    public fun template(block: MenuTemplateBuilder.() -> Unit) {
        MenuTemplateBuilder(this).apply(block)
    }

    public fun onOpen(block: suspend (MenuSession) -> Unit) {
        openHandlers += block
    }

    public fun onClose(block: suspend (MenuSession) -> Unit) {
        closeHandlers += block
    }

    public fun build(): Menu {
        require(title.isNotBlank()) { "Menu title cannot be blank" }
        require(rows in 1..6) { "Menu rows must be between 1 and 6, got $rows" }

        val definitions: Array<SlotDefinition?> = arrayOfNulls(size)
        slots.forEach { (slot, definition) ->
            validateSlotIndex(slot)
            definitions[slot] = definition
        }

        return Menu(
            title = title.mm(),
            rows = rows,
            baseSlots = definitions,
            openHandlers = openHandlers.toList(),
            closeHandlers = closeHandlers.toList(),
            pagination = pagination,
        )
    }

    internal fun validateSlotIndex(slot: Int) {
        require(slot in 0 until size) { "Slot $slot is out of range (0-${size - 1})" }
    }

    internal fun setSlotDefinition(
        slot: Int,
        definition: SlotDefinition,
        overwrite: Boolean = true,
    ) {
        validateSlotIndex(slot)
        if (overwrite) {
            slots[slot] = definition
        } else {
            slots.putIfAbsent(slot, definition)
        }
    }

    internal fun removeSlotDefinition(slot: Int) {
        validateSlotIndex(slot)
        slots.remove(slot)
    }

    internal fun topRowSlots(): IntRange = 0..8

    internal fun bottomRowSlots(): IntRange {
        val start = (rows - 1) * 9
        return start until start + 9
    }
}

/**
 * Builder for a single slot definition.
 */
public class SlotBuilder {
    public var item: ItemStack? = null
        set(value) {
            field = value?.clone()
        }

    private var renderer: (MenuRenderContext.() -> ItemStack?)? = null
    private var refreshTicks: Long? = null
    private val clickBindings = mutableListOf<MenuClickBinding>()

    public fun item(
        material: Material,
        block: ItemBuilder.() -> Unit = {},
    ) {
        item = ItemBuilder(material).apply(block).build()
    }

    public fun render(block: MenuRenderContext.() -> ItemStack?) {
        renderer = block
    }

    public fun refreshEvery(ticks: Long) {
        require(ticks > 0) { "Refresh interval must be greater than 0 ticks" }
        refreshTicks = ticks
    }

    @JvmName("onClickContext")
    public fun onClick(handler: suspend MenuClickContext.() -> Unit) {
        addClickBinding({ true }, onMenuClick(handler))
    }

    public fun onPlayerClick(handler: suspend Player.() -> Unit) {
        addClickBinding(
            { true },
            onMenuClick {
                player.handler()
            },
        )
    }

    @JvmName("onClickPlayerAndClickType")
    public fun onClick(handler: suspend (Player, ClickType) -> Unit) {
        addClickBinding(
            { true },
            onMenuClick {
                handler(player, clickType)
            },
        )
    }

    public fun onClick(action: MenuClickAction) {
        addClickBinding({ true }, action)
    }

    public fun onLeftClick(handler: suspend MenuClickContext.() -> Unit) {
        addClickBinding({ clickType -> clickType.isLeftClick }, onMenuClick(handler))
    }

    public fun onRightClick(handler: suspend MenuClickContext.() -> Unit) {
        addClickBinding({ clickType -> clickType.isRightClick }, onMenuClick(handler))
    }

    public fun onShiftClick(handler: suspend MenuClickContext.() -> Unit) {
        addClickBinding({ clickType -> clickType.isShiftClick }, onMenuClick(handler))
    }

    public fun onMiddleClick(handler: suspend MenuClickContext.() -> Unit) {
        addClickBinding({ clickType -> clickType == ClickType.MIDDLE }, onMenuClick(handler))
    }

    public fun onDropClick(handler: suspend MenuClickContext.() -> Unit) {
        addClickBinding(
            { clickType -> clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP },
            onMenuClick(handler),
        )
    }

    public fun closeOnClick() {
        onClick(
            onMenuClick {
                close()
            },
        )
    }

    public fun invalidateOnClick(vararg slots: Int) {
        onClick(
            onMenuClick {
                if (slots.isEmpty()) {
                    invalidate()
                } else {
                    invalidate(*slots)
                }
            },
        )
    }

    internal fun hasVisualState(): Boolean = item != null || renderer != null

    internal fun hasClickBindings(): Boolean = clickBindings.isNotEmpty()

    internal fun build(): SlotDefinition {
        if (refreshTicks != null) {
            require(renderer != null) { "Slot refreshEvery() requires a render { ... } block" }
        }
        return SlotDefinition(
            item = item,
            renderer = renderer,
            clickBindings = clickBindings.toList(),
            refreshTicks = refreshTicks,
        )
    }

    private fun addClickBinding(
        matcher: (ClickType) -> Boolean,
        action: MenuClickAction,
    ) {
        clickBindings += MenuClickBinding(matcher, action)
    }
}

/**
 * Mapping for pattern-based menu layouts.
 */
public class PatternMapping {
    private val mappings = mutableMapOf<Char, SlotBuilder.() -> Unit>()

    public infix fun Char.to(block: SlotBuilder.() -> Unit) {
        mappings[this] = block
    }

    internal fun getBuilder(char: Char): (SlotBuilder.() -> Unit)? = mappings[char]
}
