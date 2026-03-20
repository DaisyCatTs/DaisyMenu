@file:JvmName("DaisyMenu")
@file:Suppress("unused")

package cat.daisy.menu

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.plugin.Plugin
import java.util.logging.Level

/**
 * Entry point and runtime owner for DaisyMenu.
 */
public object DaisyMenu {
    private val blockedBottomActions =
        setOf(
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
        )

    private val blockedBottomClicks =
        setOf(
            ClickType.DOUBLE_CLICK,
        )

    private var plugin: Plugin? = null
    private var scope: CoroutineScope? = null
    private var listener: Listener? = null
    private val activeSessions = LinkedHashSet<MenuSession>()

    public fun initialize(
        pluginInstance: Plugin,
        scope: CoroutineScope? = null,
    ) {
        check(plugin == null) { "DaisyMenu is already initialized. Call DaisyMenu.shutdown() before initializing again." }

        plugin = pluginInstance
        val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                logFailure("menu coroutine", throwable)
            }
        val rootJob = SupervisorJob(scope?.coroutineContext?.get(Job))
        this.scope = CoroutineScope(BukkitDispatcher() + rootJob + exceptionHandler)

        listener =
            object : Listener {
                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
                fun onInventoryClick(event: InventoryClickEvent) {
                    val session = resolveSession(event) ?: return
                    if (event.whoClicked.uniqueId != session.player.uniqueId) {
                        event.isCancelled = true
                        return
                    }

                    val rawSlot = event.rawSlot
                    if (rawSlot < 0) {
                        return
                    }
                    if (rawSlot in 0 until session.menu.size) {
                        event.isCancelled = true
                        session.handleTopClick(rawSlot, event.click)
                        return
                    }

                    if (shouldCancelBottomInteraction(event)) {
                        event.isCancelled = true
                    }
                }

                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
                fun onInventoryDrag(event: InventoryDragEvent) {
                    val session = resolveSession(event) ?: return
                    if (event.rawSlots.any { it in 0 until session.menu.size }) {
                        event.isCancelled = true
                    }
                }

                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
                fun onInventoryClose(event: InventoryCloseEvent) {
                    val holder = event.inventory.holder as? MenuInventoryHolder ?: return
                    if (event.player.uniqueId != holder.session.player.uniqueId) {
                        return
                    }
                    holder.session.handleInventoryClose()
                }
            }

        Bukkit.getPluginManager().registerEvents(listener ?: error("Listener creation failed"), pluginInstance)
    }

    public fun shutdown() {
        val sessions = activeSessions.toList()
        sessions.forEach(MenuSession::handleInventoryClose)
        activeSessions.clear()
        listener?.let(HandlerList::unregisterAll)
        listener = null
        scope?.cancel()
        scope = null
        plugin = null
    }

    public fun isInitialized(): Boolean = plugin != null

    public fun getOpenMenuCount(): Int = activeSessions.size

    internal fun createSessionScope(name: String): CoroutineScope {
        val rootScope = scope()
        val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                logFailure(name, throwable)
            }
        return CoroutineScope(BukkitDispatcher() + SupervisorJob(rootScope.coroutineContext[Job]) + exceptionHandler)
    }

    internal fun registerSession(session: MenuSession) {
        activeSessions += session
    }

    internal fun unregisterSession(session: MenuSession) {
        activeSessions -= session
    }

    internal fun runOnMain(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            block()
        } else {
            Bukkit.getScheduler().runTask(plugin(), Runnable(block))
        }
    }

    internal fun plugin(): Plugin = plugin ?: error("DaisyMenu is not initialized. Call DaisyMenu.initialize(plugin) first.")

    internal fun scope(): CoroutineScope = scope ?: error("DaisyMenu is not initialized. Call DaisyMenu.initialize(plugin) first.")

    internal fun logFailure(
        operation: String,
        throwable: Throwable,
    ) {
        plugin?.logger?.log(Level.SEVERE, "DaisyMenu failed during $operation", throwable)
    }

    private fun resolveSession(event: InventoryClickEvent): MenuSession? {
        val holder = event.view.topInventory.holder as? MenuInventoryHolder ?: return null
        return holder.session
    }

    private fun resolveSession(event: InventoryDragEvent): MenuSession? {
        val holder = event.view.topInventory.holder as? MenuInventoryHolder ?: return null
        return holder.session
    }

    private fun shouldCancelBottomInteraction(event: InventoryClickEvent): Boolean {
        if (event.action in blockedBottomActions) {
            return true
        }
        if (event.click in blockedBottomClicks) {
            return true
        }
        return false
    }
}
