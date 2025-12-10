package cat.daisy.menu

import cat.daisy.menu.text.DaisyText.mm
import kotlinx.coroutines.suspendCancellableCoroutine
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.view.AnvilView
import kotlin.coroutines.resume

/**
 * Anvil menu for text input with suspend/return pattern.
 *
 * Usage:
 * ```kotlin
 * val name = player.openAnvil("Enter Name")
 * if (name != null) {
 *     player.sendMessage("You entered: $name")
 * }
 * ```
 */
class AnvilMenu(
    val title: String,
    val placeholder: String = "",
) {
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    suspend fun open(player: Player): String? =
        suspendCancellableCoroutine { continuation ->
            // Create anvil inventory
            val anvilView = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL, title.mm())

            // Set input slot with placeholder
            val inputItem = ItemStack(Material.PAPER)
            val meta = inputItem.itemMeta
            if (meta != null && placeholder.isNotEmpty()) {
                meta.displayName(placeholder.mm())
                inputItem.itemMeta = meta
            }
            anvilView.setItem(0, inputItem)

            // Monitor close
            val closeListener =
                object : Listener {
                    @EventHandler
                    fun onClose(event: InventoryCloseEvent) {
                        if (event.inventory != anvilView) return

                        // Unregister the event listener
                        HandlerList.unregisterAll(this)

                        // Try to get text from the anvil view's rename text
                        val outputText =
                            try {
                                val view = event.view
                                if (view is AnvilView) {
                                    view.renameText?.takeIf { it.isNotBlank() }
                                } else {
                                    // Fallback: try to get from output slot's display name
                                    val outputItem = anvilView.getItem(2)
                                    outputItem?.itemMeta?.displayName()?.let { component ->
                                        plainSerializer.serialize(component).takeIf { it.isNotBlank() }
                                    }
                                }
                            } catch (e: Exception) {
                                null
                            }

                        continuation.resume(outputText)
                    }
                }

            Bukkit.getPluginManager().registerEvents(closeListener, DaisyMenu.getPlugin())

            // Schedule open on next tick
            Bukkit.getScheduler().scheduleSyncDelayedTask(DaisyMenu.getPlugin()) {
                player.openInventory(anvilView)
            }
        }
}
