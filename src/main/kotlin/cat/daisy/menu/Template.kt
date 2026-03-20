package cat.daisy.menu

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Build-time menu layout helpers for common menu structure.
 */
public class MenuTemplateBuilder internal constructor(
    private val menuBuilder: MenuBuilder,
) {
    private val templateOwnedSlots = mutableSetOf<Int>()

    public fun border(
        material: Material = Material.GRAY_STAINED_GLASS_PANE,
        block: ItemBuilder.() -> Unit = {},
    ) {
        val definition = SlotBuilder().apply { item(material, block) }.build()
        menuBuilder.topRowSlots().forEach { slot ->
            setTemplateSlot(slot, definition)
        }
        menuBuilder.bottomRowSlots().forEach { slot ->
            setTemplateSlot(slot, definition)
        }
        for (row in 1 until menuBuilder.rows - 1) {
            setTemplateSlot(row * 9, definition)
            setTemplateSlot((row * 9) + 8, definition)
        }
    }

    public fun corners(block: SlotBuilder.() -> Unit) {
        val definition = SlotBuilder().apply(block).build()
        cornerSlots().forEach { slot ->
            setTemplateSlot(slot, definition)
        }
    }

    public fun navBar(
        row: Int,
        block: NavigationBarBuilder.() -> Unit,
    ) {
        require(row in 1..menuBuilder.rows) { "Row must be between 1 and ${menuBuilder.rows}" }
        NavigationBarBuilder(menuBuilder, row, this).apply(block).applyToMenu()
    }

    public fun content(slots: IntRange) {
        slots.forEach { slot ->
            menuBuilder.validateSlotIndex(slot)
            if (templateOwnedSlots.remove(slot)) {
                menuBuilder.removeSlotDefinition(slot)
            }
        }
    }

    internal fun setTemplateSlot(
        slot: Int,
        definition: SlotDefinition,
    ) {
        menuBuilder.setSlotDefinition(slot, definition)
        templateOwnedSlots += slot
    }

    private fun cornerSlots(): Set<Int> {
        val lastRowStart = (menuBuilder.rows - 1) * 9
        return setOf(0, 8, lastRowStart, lastRowStart + 8)
    }
}

/**
 * Template helper for common page navigation layouts.
 */
public class NavigationBarBuilder internal constructor(
    private val menuBuilder: MenuBuilder,
    private val row: Int,
    private val templateBuilder: MenuTemplateBuilder,
) {
    private val placements = linkedMapOf<Int, SlotDefinition>()

    public fun previous(
        column: Int = 1,
        block: SlotBuilder.() -> Unit = {},
    ) {
        place(
            column,
            navigationDefinition(
                defaultName = "&cPrevious",
                visible = { currentPage > 0 },
                block = block,
            ) {
                previousPage()
            },
        )
    }

    public fun next(
        column: Int = 9,
        block: SlotBuilder.() -> Unit = {},
    ) {
        place(
            column,
            navigationDefinition(
                defaultName = "&aNext",
                visible = { currentPage < totalPages - 1 },
                block = block,
            ) {
                nextPage()
            },
        )
    }

    public fun pageLabel(
        column: Int = 5,
        block: (currentPage: Int, totalPages: Int) -> ItemStack = { currentPage, totalPages ->
            item(Material.PAPER) {
                name = "&fPage &e$currentPage&7/&e$totalPages"
            }
        },
    ) {
        place(
            column,
            SlotBuilder()
                .apply {
                    render {
                        block(currentPage + 1, totalPages)
                    }
                }.build(),
        )
    }

    internal fun applyToMenu() {
        placements.forEach { (slot, definition) ->
            templateBuilder.setTemplateSlot(slot, definition)
        }
    }

    private fun place(
        column: Int,
        definition: SlotDefinition,
    ) {
        require(column in 1..9) { "Column must be between 1 and 9" }
        val index = (row - 1) * 9 + (column - 1)
        menuBuilder.validateSlotIndex(index)
        placements[index] = definition
    }

    private fun navigationDefinition(
        defaultName: String,
        visible: MenuRenderContext.() -> Boolean,
        block: SlotBuilder.() -> Unit,
        action: suspend MenuClickContext.() -> Unit,
    ): SlotDefinition {
        val builder = SlotBuilder().apply(block)
        if (!builder.hasVisualState()) {
            builder.render {
                if (!visible()) {
                    return@render null
                }
                item(Material.ARROW) {
                    name = defaultName
                }
            }
        }
        if (!builder.hasClickBindings()) {
            builder.onClick(action)
        }
        return builder.build()
    }
}
