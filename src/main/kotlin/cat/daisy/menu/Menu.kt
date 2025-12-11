package cat.daisy.menu

import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Represents an in-game menu/GUI inventory.
 * Built using DSL via [MenuBuilder].
 */
public class Menu(
    public val title: Component,
    public val rows: Int,
    internal val slots: MutableMap<Int, Button> = mutableMapOf(),
    internal val openCallbacks: MutableList<suspend (Menu) -> Unit> = mutableListOf(),
    internal val closeCallbacks: MutableList<suspend (Menu) -> Unit> = mutableListOf(),
    internal val paginationHandler: PaginationHandler? = null,
) {
    public lateinit var inventory: Inventory
    public lateinit var viewer: Player
    private val updateTasks = mutableListOf<org.bukkit.scheduler.BukkitTask>()
    private var isClosed = false

    // ─────────────────────────────────────────────────────────────────────────
    // MENU LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    public suspend fun open(player: Player) {
        require(rows in 1..6) { "Menu rows must be between 1 and 6, got $rows" }

        this.viewer = player
        val bukkitInventory = Bukkit.createInventory(player, rows * 9, title)
        this.inventory = bukkitInventory

        // Set initial items
        slots.forEach { (slot, button) ->
            require(slot in 0 until rows * 9) { "Slot $slot is out of range (0-${rows * 9 - 1})" }
            bukkitInventory.setItem(slot, button.itemStack)
        }

        // Register with manager (uses global listener, no per-menu listeners)
        DaisyMenu.registerMenu(this, bukkitInventory)

        // Call open callbacks
        openCallbacks.forEach { it.invoke(this) }

        // Open inventory on next tick
        Bukkit.getScheduler().scheduleSyncDelayedTask(DaisyMenu.getPlugin()) {
            player.openInventory(bukkitInventory)
        }
    }

    public fun close() {
        if (!isClosed) {
            isClosed = true
            viewer.closeInventory()
        }
    }

    internal fun invokeClose() {
        updateTasks.forEach { it.cancel() }
        DaisyMenu.getScope().launch {
            closeCallbacks.forEach { it.invoke(this@Menu) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update a specific slot with a new item.
     */
    public fun updateSlot(
        slot: Int,
        button: Button,
    ) {
        slots[slot] = button
        inventory.setItem(slot, button.itemStack)
    }

    /**
     * Repeat a task every [ticks] server ticks.
     * Automatically cancelled when menu closes.
     * Executes on main thread.
     */
    public fun repeatUpdate(
        ticks: Long,
        block: suspend () -> Unit,
    ) {
        val task =
            Bukkit.getScheduler().runTaskTimer(
                DaisyMenu.getPlugin(),
                Runnable {
                    DaisyMenu.getScope().launch {
                        block.invoke()
                    }
                },
                0L,
                ticks,
            )
        updateTasks.add(task)
    }

    /**
     * Update a specific slot using DSL builder.
     *
     * ```kotlin
     * menu.updateSlot(13) {
     *     item(Material.DIAMOND) { name("Updated!") }
     * }
     * ```
     */
    public fun updateSlot(
        slot: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val slotBuilder = SlotBuilder()
        slotBuilder.apply(block)
        val button = slotBuilder.build()
        slots[slot] = button
        inventory.setItem(slot, button.itemStack)
    }

    /**
     * Fill empty slots with a button at runtime.
     */
    public fun fill(button: Button) {
        for (i in 0 until (rows * 9)) {
            if (!slots.containsKey(i)) {
                slots[i] = button
                inventory.setItem(i, button.itemStack)
            }
        }
    }
}
