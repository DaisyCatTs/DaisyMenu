@file:JvmName("DaisyMenu")
@file:Suppress("unused")

package cat.daisy.menu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.plugin.Plugin

/**
 * # DaisyMenu - Modern Kotlin GUI DSL for Paper/Bukkit
 *
 * A high-performance, coroutine-first GUI framework featuring:
 * - **Beautiful Kotlin DSL** - Clean, expressive syntax
 * - **Suspendable Handlers** - Full coroutine support for clicks and events
 * - **Anvil Input** - Built-in anvil GUI with suspend/return pattern
 * - **Pagination** - Multi-page menus with automatic navigation
 * - **Live Updates** - Coroutine-based dynamic menu updates
 * - **Text Integration** - Automatic MiniMessage formatting via DaisyText
 * - **Dupe Prevention** - Complete protection against item duplication exploits
 * - **Paper Optimized** - Built specifically for Paper and its forks
 *
 * ## Quick Start
 * ```kotlin
 * player.openMenu {
 *     title = "Shop"
 *     rows = 3
 *
 *     slot(13) {
 *         item(Material.DIAMOND) { name("Buy") }
 *         onClick { player -> player.sendMessage("Purchased!") }
 *     }
 * }
 * ```
 *
 * ## Anvil Input
 * ```kotlin
 * val name = player.openAnvil("Rename Item")
 * if (name != null) {
 *     player.sendMessage("You entered: $name")
 * }
 * ```
 */
object DaisyMenu {
    private var plugin: Plugin? = null
    private var coroutineScope: CoroutineScope? = null
    private val menus = mutableMapOf<Int, Menu>()

    // Actions that could lead to duplication if not blocked
    private val BLOCKED_ACTIONS =
        setOf(
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.CLONE_STACK,
            InventoryAction.SWAP_WITH_CURSOR,
        )

    /**
     * Initialize DaisyMenu with your plugin instance.
     * Must be called before using any DaisyMenu features.
     *
     * @param pluginInstance Your plugin instance
     * @param scope Optional custom CoroutineScope (defaults to main thread dispatcher)
     */
    fun initialize(
        pluginInstance: Plugin,
        scope: CoroutineScope? = null,
    ) {
        plugin = pluginInstance
        coroutineScope = scope ?: CoroutineScope(getBukkitDispatcher())

        // Register single global listener for all menus - highest priority for security
        val globalListener =
            object : Listener {
                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
                fun onInventoryClick(event: InventoryClickEvent) {
                    val menu = menus.values.firstOrNull { it.inventory == event.inventory } ?: return

                    // DUPE PREVENTION: Cancel ALL interactions with menu inventory
                    event.isCancelled = true

                    // Block dangerous actions completely
                    if (event.action in BLOCKED_ACTIONS) {
                        return
                    }

                    // Only process clicks on the top inventory (menu itself)
                    val slot = event.rawSlot
                    if (slot < 0 || slot >= menu.rows * 9) {
                        // Click was in player inventory - allow viewing but prevent interaction
                        return
                    }

                    // Prevent shift-click exploits
                    if (event.isShiftClick) {
                        return
                    }

                    // Get the button and invoke click handler
                    val button = menu.slots[slot] ?: return

                    // Verify the clicker is the menu owner (prevents ghost inventory exploits)
                    val player = event.whoClicked as? Player ?: return
                    if (player.uniqueId != menu.viewer.uniqueId) {
                        return
                    }

                    getScope().launch {
                        button.invokeClick(player, event.click)
                    }
                }

                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
                fun onInventoryDrag(event: InventoryDragEvent) {
                    // DUPE PREVENTION: Block all drag events on menus
                    val menu = menus.values.firstOrNull { it.inventory == event.inventory } ?: return

                    // If any slot is in the menu area, cancel
                    val menuSlots = 0 until menu.rows * 9
                    if (event.rawSlots.any { it in menuSlots }) {
                        event.isCancelled = true
                    }
                }

                @EventHandler(priority = EventPriority.MONITOR)
                fun onInventoryClose(event: InventoryCloseEvent) {
                    val menu = menus.remove(System.identityHashCode(event.inventory)) ?: return

                    // Clear cursor to prevent item duplication
                    val player = event.player as? Player
                    if (player != null && !player.itemOnCursor.type.isAir) {
                        // Drop cursor item instead of duping
                        player.world.dropItemNaturally(player.location, player.itemOnCursor)
                        player.setItemOnCursor(null)
                    }

                    menu.invokeClose()
                }
            }

        Bukkit.getPluginManager().registerEvents(globalListener, pluginInstance)
    }

    /**
     * Shutdown DaisyMenu, closing all menus and cleaning up.
     * Call this in your plugin's onDisable().
     */
    fun shutdown() {
        // Close all open menus safely
        menus.values.toList().forEach { menu ->
            try {
                menu.viewer.closeInventory()
            } catch (_: Exception) {
                // Player may be offline
            }
        }
        menus.clear()
        plugin = null
        coroutineScope = null
    }

    /**
     * Check if DaisyMenu has been initialized.
     */
    fun isInitialized(): Boolean = plugin != null

    /**
     * Get the number of currently open menus.
     */
    fun getOpenMenuCount(): Int = menus.size

    @PublishedApi
    internal fun getPlugin(): Plugin = plugin ?: error("DaisyMenu not initialized. Call DaisyMenu.initialize(plugin) in onEnable()")

    @PublishedApi
    internal fun getScope(): CoroutineScope =
        coroutineScope ?: error("DaisyMenu not initialized. Call DaisyMenu.initialize(plugin) in onEnable()")

    @PublishedApi
    internal fun registerMenu(
        menu: Menu,
        inventory: org.bukkit.inventory.Inventory,
    ) {
        menus[System.identityHashCode(inventory)] = menu
    }
}
