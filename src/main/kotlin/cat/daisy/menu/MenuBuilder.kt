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

    public fun slot(
        index: Int,
        button: Button,
    ) {
        slots[index] = button.definition
    }

    public fun slot(
        index: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val builder = SlotBuilder()
        builder.apply(block)
        slots[index] = builder.build()
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
        repeat(rows * 9) { slot ->
            slots.putIfAbsent(slot, definition)
        }
    }

    public fun fillSlots(
        vararg indices: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val definition = SlotBuilder().apply(block).build()
        indices.forEach { slots[it] = definition }
    }

    public fun fillRow(
        row: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(row in 1..rows) { "Row must be between 1 and $rows" }
        val definition = SlotBuilder().apply(block).build()
        val start = (row - 1) * 9
        for (slot in start until start + 9) {
            slots[slot] = definition
        }
    }

    public fun fillColumn(
        column: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(column in 1..9) { "Column must be between 1 and 9" }
        val definition = SlotBuilder().apply(block).build()
        for (row in 0 until rows) {
            slots[(row * 9) + (column - 1)] = definition
        }
    }

    public fun fillBorder(
        material: Material = Material.GRAY_STAINED_GLASS_PANE,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val definition = SlotBuilder().apply { item(material, block) }.build()
        for (slot in 0 until 9) {
            slots[slot] = definition
        }
        val bottomStart = (rows - 1) * 9
        for (slot in bottomStart until bottomStart + 9) {
            slots[slot] = definition
        }
        for (row in 1 until rows - 1) {
            slots[row * 9] = definition
            slots[(row * 9) + 8] = definition
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

    public fun onOpen(block: suspend (MenuSession) -> Unit) {
        openHandlers += block
    }

    public fun onClose(block: suspend (MenuSession) -> Unit) {
        closeHandlers += block
    }

    public fun build(): Menu {
        require(title.isNotBlank()) { "Menu title cannot be blank" }
        require(rows in 1..6) { "Menu rows must be between 1 and 6, got $rows" }

        val size = rows * 9
        val definitions: Array<SlotDefinition?> = arrayOfNulls(size)
        slots.forEach { (slot, definition) ->
            require(slot in 0 until size) { "Slot $slot is out of range (0-${size - 1})" }
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
}

/**
 * Builder for a single slot definition.
 */
public class SlotBuilder {
    public var item: ItemStack? = null
        set(value) {
            field = value?.clone()
        }

    private var renderer: (MenuRenderContext.() -> ItemStack)? = null
    private var clickHandler: (suspend MenuClickContext.() -> Unit)? = null
    private var refreshTicks: Long? = null

    public fun item(
        material: Material,
        block: ItemBuilder.() -> Unit = {},
    ) {
        item = ItemBuilder(material).apply(block).build()
    }

    public fun render(block: MenuRenderContext.() -> ItemStack) {
        renderer = block
    }

    public fun refreshEvery(ticks: Long) {
        require(ticks > 0) { "Refresh interval must be greater than 0 ticks" }
        refreshTicks = ticks
    }

    @JvmName("onClickContext")
    public fun onClick(handler: suspend MenuClickContext.() -> Unit) {
        clickHandler = handler
    }

    @JvmName("onClickPlayer")
    public fun onClick(handler: suspend (Player) -> Unit) {
        clickHandler = { handler(player) }
    }

    @JvmName("onClickPlayerAndClickType")
    public fun onClick(handler: suspend (Player, ClickType) -> Unit) {
        clickHandler = { handler(player, clickType) }
    }

    internal fun build(): SlotDefinition {
        if (refreshTicks != null) {
            require(renderer != null) { "Slot refreshEvery() requires a render { ... } block" }
        }
        return SlotDefinition(
            item = item,
            renderer = renderer,
            clickHandler = clickHandler,
            refreshTicks = refreshTicks,
        )
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
