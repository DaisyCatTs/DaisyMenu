package cat.daisy.menu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Immutable menu definition that can be opened for many viewers.
 */
public class Menu internal constructor(
    public val title: Component,
    public val rows: Int,
    internal val baseSlots: Array<SlotDefinition?>,
    internal val openHandlers: List<suspend (MenuSession) -> Unit>,
    internal val closeHandlers: List<suspend (MenuSession) -> Unit>,
    internal val pagination: PaginationDefinition?,
) {
    public val size: Int = rows * 9

    public fun open(player: Player): MenuSession = MenuSession(this, player).also(MenuSession::open)
}

/**
 * Build an immutable [Menu] with a Kotlin DSL.
 */
public fun menu(
    title: String,
    rows: Int = 3,
    block: MenuBuilder.() -> Unit,
): Menu = MenuBuilder(title, rows).apply(block).build()

/**
 * Per-viewer runtime session for an opened [Menu].
 */
public class MenuSession internal constructor(
    public val menu: Menu,
    public val player: Player,
) {
    private val renderMutex = Mutex()
    private val holder = MenuInventoryHolder(this)
    private val sessionScope: CoroutineScope = DaisyMenu.createSessionScope("menu:${player.uniqueId}")
    private val activeDefinitions: Array<SlotDefinition?> = arrayOfNulls(menu.size)
    private val renderedItems: Array<ItemStack?> = arrayOfNulls(menu.size)
    private val slotRefreshes: Array<ManagedRefresh?> = arrayOfNulls(menu.size)
    private val sessionRefreshes = mutableListOf<ManagedRefresh>()
    private var closed = false
    private var page: Int = 0
    private var totalPages: Int = 1

    public val inventory: Inventory = Bukkit.createInventory(holder, menu.size, menu.title)
    public val currentPage: Int
        get() = page

    public fun hasOpenInventory(): Boolean = player.openInventory.topInventory.holder === holder

    internal val pageCount: Int
        get() = totalPages

    internal fun open() {
        sessionScope.launch {
            renderAll()
            if (closed) {
                return@launch
            }
            DaisyMenu.registerSession(this@MenuSession)
            player.openInventory(inventory)
            invokeHandlers(menu.openHandlers, "open")
        }
    }

    public fun close() {
        if (closed) {
            return
        }
        DaisyMenu.runOnMain {
            if (!closed && hasOpenInventory()) {
                player.closeInventory()
            }
        }
    }

    public fun invalidate() {
        if (closed) {
            return
        }
        sessionScope.launch {
            renderAll()
        }
    }

    public fun invalidate(slot: Int) {
        invalidateSlots(intArrayOf(slot))
    }

    public fun invalidate(vararg slots: Int) {
        invalidateSlots(slots)
    }

    private fun invalidateSlots(slots: IntArray) {
        slots.forEach { slot ->
            require(slot in 0 until menu.size) { "Slot $slot is out of range (0-${menu.size - 1})" }
        }
        if (closed) {
            return
        }
        val uniqueSlots = slots.distinct().toIntArray()
        sessionScope.launch {
            renderSlots(uniqueSlots)
        }
    }

    public fun refreshEvery(
        ticks: Long,
        block: suspend MenuSession.() -> Unit,
    ): Cancellable {
        require(ticks > 0) { "Refresh interval must be greater than 0 ticks" }
        val managedRefresh =
            ManagedRefresh(
                ticks = ticks,
                sessionScope = sessionScope,
                invoke = {
                    block(this)
                },
            )
        sessionRefreshes += managedRefresh
        managedRefresh.start()
        return managedRefresh
    }

    internal fun handleTopClick(
        slot: Int,
        clickType: ClickType,
    ) {
        if (closed) {
            return
        }
        val action = activeDefinitions.getOrNull(slot)?.resolveClick(clickType) ?: return
        sessionScope.launch {
            action.invoke(MenuClickContext(this@MenuSession, slot, clickType))
        }
    }

    internal fun handleInventoryClose() {
        if (closed) {
            return
        }
        closed = true
        DaisyMenu.unregisterSession(this)
        cancelRefreshes()
        sessionScope.cancel()
        DaisyMenu.scope().launch {
            invokeHandlers(menu.closeHandlers, "close")
        }
    }

    internal suspend fun previousPage() {
        val target = (page - 1).coerceAtLeast(0)
        if (target == page) {
            return
        }
        page = target
        renderAll()
    }

    internal suspend fun nextPage() {
        val target = (page + 1).coerceAtMost(totalPages - 1)
        if (target == page) {
            return
        }
        page = target
        renderAll()
    }

    internal fun clampPage(pageCount: Int) {
        page = page.coerceIn(0, pageCount.coerceAtLeast(1) - 1)
    }

    internal fun updatePageCount(pageCount: Int) {
        totalPages = pageCount.coerceAtLeast(1)
        clampPage(totalPages)
    }

    private suspend fun renderAll() {
        renderSlots((0 until menu.size).toList().toIntArray())
    }

    private suspend fun renderSlots(slots: IntArray) {
        if (closed) {
            return
        }
        renderMutex.withLock {
            if (closed) {
                return
            }

            val nextDefinitions = buildDefinitions()
            slots.forEach { slot ->
                val definition = nextDefinitions[slot]
                syncSlotRefresh(slot, definition)
                val nextItem = definition?.render(this, slot)
                activeDefinitions[slot] = if (nextItem == null) null else definition
                applyRenderedItem(slot, nextItem)
            }
        }
    }

    private suspend fun buildDefinitions(): Array<SlotDefinition?> {
        val definitions = menu.baseSlots.copyOf()
        val pagination = menu.pagination ?: return definitions.also { totalPages = 1 }
        val scope = PaginationScope(this, pagination.itemsPerPage)
        pagination.block.invoke(scope)
        scope.pageSlots.forEach { (slot, definition) ->
            definitions[slot] = definition
        }
        return definitions
    }

    private fun applyRenderedItem(
        slot: Int,
        nextItem: ItemStack?,
    ) {
        val previous = renderedItems[slot]
        if (previous == nextItem) {
            return
        }
        renderedItems[slot] = nextItem?.clone()
        inventory.setItem(slot, nextItem?.clone())
    }

    private fun syncSlotRefresh(
        slot: Int,
        definition: SlotDefinition?,
    ) {
        val existing = slotRefreshes[slot]
        val refreshTicks = definition?.refreshTicks
        val renderer = definition?.renderer
        if (refreshTicks == null || renderer == null) {
            existing?.cancel()
            slotRefreshes[slot] = null
            return
        }

        if (existing != null && existing.matches(definition, refreshTicks)) {
            return
        }

        existing?.cancel()
        val managedRefresh =
            ManagedRefresh(
                ticks = refreshTicks,
                sessionScope = sessionScope,
                definition = definition,
                invoke = {
                    invalidate(slot)
                },
            )
        managedRefresh.start()
        slotRefreshes[slot] = managedRefresh
    }

    private suspend fun invokeHandlers(
        handlers: List<suspend (MenuSession) -> Unit>,
        phase: String,
    ) {
        handlers.forEach { handler ->
            runCatching {
                handler(this)
            }.onFailure { throwable ->
                DaisyMenu.logFailure("menu $phase callback", throwable)
            }
        }
    }

    private fun cancelRefreshes() {
        slotRefreshes.forEach { it?.cancel() }
        sessionRefreshes.forEach(ManagedRefresh::cancel)
        sessionRefreshes.clear()
    }
}

public fun interface Cancellable {
    public fun cancel()
}

public class MenuRenderContext internal constructor(
    public val session: MenuSession,
    public val slot: Int,
) {
    public val player: Player
        get() = session.player
    public val menu: Menu
        get() = session.menu
    public val currentPage: Int
        get() = session.currentPage
    public val totalPages: Int
        get() = session.pageCount
}

public class MenuClickContext internal constructor(
    public val session: MenuSession,
    public val slot: Int,
    public val clickType: ClickType,
) {
    public val player: Player
        get() = session.player
    public val menu: Menu
        get() = session.menu
    public val currentPage: Int
        get() = session.currentPage
    public val totalPages: Int
        get() = session.pageCount

    public fun close() {
        session.close()
    }

    public fun invalidate() {
        session.invalidate()
    }

    public fun invalidate(slot: Int) {
        session.invalidate(slot)
    }

    public fun invalidate(vararg slots: Int) {
        session.invalidate(*slots)
    }

    public suspend fun previousPage() {
        session.previousPage()
    }

    public suspend fun nextPage() {
        session.nextPage()
    }

    public fun isShiftClick(): Boolean = clickType.isShiftClick

    public fun isLeftClick(): Boolean = clickType.isLeftClick

    public fun isRightClick(): Boolean = clickType.isRightClick
}

public class MenuClickAction internal constructor(
    private val handler: suspend MenuClickContext.() -> Unit,
) {
    internal suspend fun invoke(context: MenuClickContext) {
        handler(context)
    }
}

public fun onMenuClick(handler: suspend MenuClickContext.() -> Unit): MenuClickAction = MenuClickAction(handler)

internal class MenuClickBinding(
    private val matcher: (ClickType) -> Boolean,
    private val action: MenuClickAction,
) {
    fun matches(clickType: ClickType): Boolean = matcher(clickType)

    fun action(): MenuClickAction = action
}

internal class SlotDefinition(
    item: ItemStack? = null,
    internal val renderer: (MenuRenderContext.() -> ItemStack?)? = null,
    private val clickBindings: List<MenuClickBinding> = emptyList(),
    internal val refreshTicks: Long? = null,
) {
    private val staticItem: ItemStack? = item?.clone()

    internal fun previewItem(): ItemStack? = staticItem?.clone()

    internal fun resolveClick(clickType: ClickType): MenuClickAction? =
        clickBindings
            .firstOrNull { binding -> binding.matches(clickType) }
            ?.action()

    internal fun render(
        session: MenuSession,
        slot: Int,
    ): ItemStack? {
        if (renderer != null) {
            return renderer.invoke(MenuRenderContext(session, slot))?.clone()
        }
        return staticItem?.clone()
    }
}

internal class MenuInventoryHolder(
    val session: MenuSession,
) : InventoryHolder {
    override fun getInventory(): Inventory = session.inventory
}

private class ManagedRefresh(
    private val ticks: Long,
    private val sessionScope: CoroutineScope,
    private val invoke: suspend () -> Unit,
    private val definition: SlotDefinition? = null,
) : Cancellable {
    private var task: org.bukkit.scheduler.BukkitTask? = null
    private var running: Boolean = false

    fun start() {
        task =
            Bukkit.getScheduler().runTaskTimer(
                DaisyMenu.plugin(),
                Runnable {
                    if (running) {
                        return@Runnable
                    }
                    running = true
                    sessionScope.launch {
                        try {
                            invoke()
                        } catch (throwable: Throwable) {
                            DaisyMenu.logFailure("menu refresh", throwable)
                        } finally {
                            running = false
                        }
                    }
                },
                ticks,
                ticks,
            )
    }

    fun matches(
        otherDefinition: SlotDefinition,
        otherTicks: Long,
    ): Boolean = definition === otherDefinition && ticks == otherTicks

    override fun cancel() {
        task?.cancel()
        task = null
        running = false
    }
}
