package cat.daisy.menu

import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.scheduler.BukkitTask

/**
 * Represents an in-game menu inventory built with [MenuBuilder].
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

    private val updateTasks = mutableListOf<BukkitTask>()
    private var isClosed = false
    private var baseSlots: MutableMap<Int, Button> = slots.toMutableMap()
    private var currentPage = 0

    public suspend fun open(player: Player) {
        require(rows in 1..6) { "Menu rows must be between 1 and 6, got $rows" }

        viewer = player
        inventory = Bukkit.createInventory(player, rows * 9, title)

        DaisyMenu.registerMenu(this, inventory)

        openCallbacks.forEach { it.invoke(this) }
        baseSlots = slots.toMutableMap()
        render()

        Bukkit.getScheduler().scheduleSyncDelayedTask(DaisyMenu.getPlugin()) {
            player.openInventory(inventory)
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

    public fun updateSlot(
        slot: Int,
        button: Button,
    ) {
        baseSlots[slot] = button
        slots[slot] = button
        inventory.setItem(slot, button.itemStack)
    }

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

    public fun updateSlot(
        slot: Int,
        block: SlotBuilder.() -> Unit,
    ) {
        val slotBuilder = SlotBuilder()
        slotBuilder.apply(block)
        updateSlot(slot, slotBuilder.build())
    }

    public fun fill(button: Button) {
        for (i in 0 until (rows * 9)) {
            if (!baseSlots.containsKey(i)) {
                baseSlots[i] = button
                slots[i] = button
                inventory.setItem(i, button.itemStack)
            }
        }
    }

    private suspend fun render() {
        inventory.clear()
        slots.clear()
        slots.putAll(baseSlots)

        paginationHandler?.let { handler ->
            val paginationBuilder = PaginationBuilder(handler.itemsPerPage)
            paginationBuilder.currentPage = currentPage
            paginationBuilder.pageChangeAction = { newPage ->
                currentPage = newPage
                render()
            }
            handler.block.invoke(paginationBuilder)
            slots.putAll(paginationBuilder.buttons)
        }

        slots.forEach { (slot, button) ->
            require(slot in 0 until rows * 9) { "Slot $slot is out of range (0-${rows * 9 - 1})" }
            inventory.setItem(slot, button.itemStack)
        }
    }
}
