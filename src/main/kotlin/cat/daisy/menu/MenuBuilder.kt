package cat.daisy.menu

import cat.daisy.menu.text.DaisyText.mm
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * DSL builder for creating menus.
 */
public class MenuBuilder {
    public lateinit var title: String
    public var rows: Int = 3
    internal val buttons = mutableMapOf<Int, Button>()
    internal val openCallbacks = mutableListOf<suspend (Menu) -> Unit>()
    internal val closeCallbacks = mutableListOf<suspend (Menu) -> Unit>()
    internal var paginationConfig: PaginationHandler? = null

    // ─────────────────────────────────────────────────────────────────────────
    // SLOT DEFINITION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Define a button at a specific slot.
     */
    public fun slot(
        index: Int,
        btn: Button,
    ) {
        buttons[index] = btn
    }

    /**
     * Define a button at a specific slot with a builder.
     */
    public fun slot(
        index: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val slotBuilder = SlotBuilder()
        slotBuilder.apply(block)
        buttons[index] = slotBuilder.build()
    }

    /**
     * Define a button using x/y coordinates (1-indexed).
     */
    public fun slot(
        x: Int,
        y: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val slot = (x - 1) + ((y - 1) * 9)
        slot(slot, block)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILL & PATTERNS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fill empty slots with a default gray stained glass pane.
     * Use the block to customize the item (name, lore, etc).
     *
     * ```kotlin
     * fill { name(" ") }
     * ```
     */
    public fun fill(block: ItemBuilder.() -> Unit = {}) {
        fill(Material.GRAY_STAINED_GLASS_PANE, block)
    }

    /**
     * Fill empty slots with a specific material.
     *
     * ```kotlin
     * fill(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
     * ```
     */
    public fun fill(
        material: Material,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val itemBuilder = ItemBuilder(material)
        itemBuilder.apply(block)
        val fillButton = Button(itemBuilder.build(), null)
        for (i in 0 until (rows * 9)) {
            if (!buttons.containsKey(i)) {
                buttons[i] = fillButton
            }
        }
    }

    /**
     * Fill specific slots with a button (for patterns).
     *
     * ```kotlin
     * fillSlots(0, 8, 45, 53) { // corners
     *     item(Material.DIAMOND) { name("&bCorner") }
     * }
     * ```
     */
    public fun fillSlots(
        vararg slots: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val slotBuilder = SlotBuilder()
        slotBuilder.apply(block)
        val btn = slotBuilder.build()
        slots.forEach { buttons[it] = btn }
    }

    /**
     * Fill a row with a button (1-indexed).
     *
     * ```kotlin
     * fillRow(1) { item(Material.RED_STAINED_GLASS_PANE) { name(" ") } }
     * ```
     */
    public fun fillRow(
        row: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(row in 1..rows) { "Row must be between 1 and $rows" }
        val slotBuilder = SlotBuilder()
        slotBuilder.apply(block)
        val btn = slotBuilder.build()
        val startSlot = (row - 1) * 9
        for (i in startSlot until startSlot + 9) {
            buttons[i] = btn
        }
    }

    /**
     * Fill a column with a button (1-indexed, 1-9).
     *
     * ```kotlin
     * fillColumn(1) { item(Material.BLUE_STAINED_GLASS_PANE) { name(" ") } }
     * ```
     */
    public fun fillColumn(
        column: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        require(column in 1..9) { "Column must be between 1 and 9" }
        val slotBuilder = SlotBuilder()
        slotBuilder.apply(block)
        val btn = slotBuilder.build()
        for (row in 0 until rows) {
            buttons[row * 9 + (column - 1)] = btn
        }
    }

    /**
     * Fill the border of the menu.
     *
     * ```kotlin
     * fillBorder(Material.WHITE_STAINED_GLASS_PANE) { name(" ") }
     * ```
     */
    public fun fillBorder(
        material: Material = Material.GRAY_STAINED_GLASS_PANE,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val itemBuilder = ItemBuilder(material)
        itemBuilder.apply(block)
        val borderButton = Button(itemBuilder.build(), null)

        // Top row
        for (i in 0 until 9) {
            buttons[i] = borderButton
        }
        // Bottom row
        val bottomStart = (rows - 1) * 9
        for (i in bottomStart until bottomStart + 9) {
            buttons[i] = borderButton
        }
        // Left and right columns (excluding corners already filled)
        for (row in 1 until rows - 1) {
            buttons[row * 9] = borderButton // Left
            buttons[row * 9 + 8] = borderButton // Right
        }
    }

    /**
     * Apply a pattern using a string template.
     * Each character maps to a Button via the provided mapping.
     *
     * ```kotlin
     * pattern(
     *     "XXXXXXXXX",
     *     "X       X",
     *     "X   D   X",
     *     "X       X",
     *     "XXXXXXXXX"
     * ) {
     *     'X' to { item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") } }
     *     'D' to { item(Material.DIAMOND) { name("&bDiamond"); onClick { p, _ -> p.sendMessage("Clicked!") } } }
     * }
     * ```
     */
    public fun pattern(
        vararg lines: String,
        mapping: PatternMapping.() -> Unit,
    ) {
        require(lines.size <= rows) { "Pattern has ${lines.size} rows but menu only has $rows rows" }

        val patternMapping = PatternMapping()
        patternMapping.apply(mapping)

        lines.forEachIndexed { rowIndex, line ->
            line.take(9).forEachIndexed { colIndex, char ->
                if (char != ' ') {
                    patternMapping.getBuilder(char)?.let { builder ->
                        val slotBuilder = SlotBuilder()
                        slotBuilder.apply(builder)
                        buttons[rowIndex * 9 + colIndex] = slotBuilder.build()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGINATION
    // ─────────────────────────────────────────────────────────────────────────

    public fun pagination(
        itemsPerPage: Int,
        block: suspend PaginationBuilder.() -> Unit,
    ) {
        paginationConfig = PaginationHandler(itemsPerPage, block)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when menu is opened.
     */
    public fun onOpen(block: suspend (Menu) -> Unit) {
        openCallbacks.add(block)
    }

    /**
     * Called when menu is closed.
     */
    public fun onClose(block: suspend (Menu) -> Unit) {
        closeCallbacks.add(block)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD
    // ─────────────────────────────────────────────────────────────────────────

    public fun build(): Menu {
        require(title.isNotEmpty()) { "Menu title cannot be empty" }
        require(rows in 1..6) { "Menu rows must be between 1 and 6, got $rows" }

        val titleComponent = title.mm()
        return Menu(titleComponent, rows, buttons, openCallbacks, closeCallbacks, paginationConfig)
    }
}

/**
 * Builder for a single slot button.
 */
public class SlotBuilder {
    private var itemStack: ItemStack? = null
    private var clickHandler: (suspend (Player, ClickType) -> Unit)? = null

    public fun item(
        material: Material,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val itemBuilder = ItemBuilder(material)
        itemBuilder.apply(block)
        this.itemStack = itemBuilder.build()
    }

    public fun item(itemStack: ItemStack) {
        this.itemStack = itemStack
    }

    // Accept suspend function with both parameters
    public fun onClick(handler: suspend (Player, ClickType) -> Unit) {
        this.clickHandler = handler
    }

    // Accept suspend function with just Player parameter
    public fun onClick(handler: suspend (Player) -> Unit) {
        this.clickHandler = { player, _ -> handler(player) }
    }

    // Accept regular function with both parameters (auto-wrapped in coroutine)
    public fun onClickSync(handler: (Player, ClickType) -> Unit) {
        this.clickHandler = { player, clickType -> handler(player, clickType) }
    }

    // Accept regular function with just Player parameter (auto-wrapped in coroutine)
    public fun onClickSync(handler: (Player) -> Unit) {
        this.clickHandler = { player, _ -> handler(player) }
    }

    public fun build(): Button {
        val stack = itemStack ?: ItemStack(Material.AIR)
        return Button(stack, clickHandler)
    }
}

/**
 * Mapping for pattern-based menu layouts.
 * Maps characters to SlotBuilder configurations.
 */
public class PatternMapping {
    private val mappings = mutableMapOf<Char, SlotBuilder.() -> Unit>()

    /**
     * Map a character to a slot builder configuration.
     */
    public infix fun Char.to(block: SlotBuilder.() -> Unit) {
        mappings[this] = block
    }

    internal fun getBuilder(char: Char): (SlotBuilder.() -> Unit)? = mappings[char]
}
