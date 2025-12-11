package cat.daisy.menu

/**
 * Handles pagination for multi-page menus.
 */
public class PaginationHandler(
    public val itemsPerPage: Int,
    public val block: suspend PaginationBuilder.() -> Unit,
)

/**
 * Builder for pagination UI.
 * Provides slot() method for defining buttons within paginated context.
 */
public class PaginationBuilder(
    public var itemsPerPage: Int = 45,
    internal val buttons: MutableMap<Int, Button> = mutableMapOf(),
) {
    public var currentPage: Int = 0
    public var totalPages: Int = 1
    private var previousAction: (suspend () -> Unit)? = null
    private var nextAction: (suspend () -> Unit)? = null

    /**
     * Set total number of pages.
     */
    public fun totalPages(count: Int) {
        this.totalPages = count
    }

    /**
     * Get items for the current page.
     */
    public fun pageItems(): IntRange {
        val start = currentPage * itemsPerPage
        val end = minOf(start + itemsPerPage, totalPages * itemsPerPage)
        return start until end
    }

    /**
     * Define a button at a specific slot within pagination context.
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
     * Register previous page action.
     */
    public fun previous(action: suspend () -> Unit) {
        previousAction = action
    }

    /**
     * Register next page action.
     */
    public fun next(action: suspend () -> Unit) {
        nextAction = action
    }

    /**
     * Move to previous page.
     */
    public suspend fun prevPage() {
        if (currentPage > 0) {
            currentPage--
            previousAction?.invoke()
        }
    }

    /**
     * Move to next page.
     */
    public suspend fun nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++
            nextAction?.invoke()
        }
    }

    /**
     * Check if previous page exists.
     */
    public fun hasPrevious(): Boolean = currentPage > 0

    /**
     * Check if next page exists.
     */
    public fun hasNext(): Boolean = currentPage < totalPages - 1
}
