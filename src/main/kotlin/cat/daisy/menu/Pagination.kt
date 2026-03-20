package cat.daisy.menu

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Runtime pagination helpers for page-aware menu content.
 */
public class PaginationScope internal constructor(
    private val session: MenuSession,
    public val itemsPerPage: Int,
) {
    internal val pageSlots = mutableMapOf<Int, SlotDefinition>()
    private var computedPageCount: Int = 1

    public val currentPage: Int
        get() = session.currentPage

    public fun pageCount(totalItems: Int): Int {
        require(totalItems >= 0) { "Total items must be at least 0" }
        computedPageCount =
            if (totalItems == 0) {
                1
            } else {
                ((totalItems - 1) / itemsPerPage) + 1
            }
        session.updatePageCount(computedPageCount)
        return computedPageCount
    }

    public fun pageRange(totalItems: Int): IntRange {
        pageCount(totalItems)
        val start = session.currentPage * itemsPerPage
        val endExclusive = minOf(totalItems, start + itemsPerPage)
        if (start >= endExclusive) {
            return IntRange.EMPTY
        }
        return start until endExclusive
    }

    public fun <T> pageItems(items: List<T>): List<T> {
        val range = pageRange(items.size)
        if (range.isEmpty()) {
            return emptyList()
        }
        return items.subList(range.first, range.last + 1)
    }

    public fun hasPrevious(): Boolean = session.currentPage > 0

    public fun hasNext(): Boolean = session.currentPage < computedPageCount - 1

    public suspend fun previousPage() {
        session.previousPage()
    }

    public suspend fun nextPage() {
        session.nextPage()
    }

    public fun slot(
        index: Int,
        button: Button,
    ) {
        pageSlots[index] = button.definition
    }

    public fun slot(
        index: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        pageSlots[index] = SlotBuilder().apply(block).build()
    }

    public fun previousButton(
        index: Int,
        block: SlotBuilder.() -> Unit = {},
    ) {
        if (!hasPrevious()) {
            return
        }
        pageSlots[index] = navigationButton("&cPrevious", block) { previousPage() }
    }

    public fun nextButton(
        index: Int,
        block: SlotBuilder.() -> Unit = {},
    ) {
        if (!hasNext()) {
            return
        }
        pageSlots[index] = navigationButton("&aNext", block) { nextPage() }
    }

    public fun pageLabel(
        index: Int,
        block: (currentPage: Int, totalPages: Int) -> ItemStack,
    ) {
        pageSlots[index] =
            SlotBuilder()
                .apply {
                    render {
                        block(currentPage + 1, totalPages)
                    }
                }.build()
    }

    private fun navigationButton(
        defaultName: String,
        block: SlotBuilder.() -> Unit,
        action: suspend MenuClickContext.() -> Unit,
    ): SlotDefinition {
        val builder =
            SlotBuilder().apply {
                item(Material.ARROW) {
                    name = defaultName
                }
            }
        builder.apply(block)
        if (!builder.hasClickBindings()) {
            builder.onClick(action)
        }
        return builder.build()
    }
}

internal class PaginationDefinition(
    val itemsPerPage: Int,
    val block: suspend PaginationScope.() -> Unit,
)
