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
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType
import org.bukkit.inventory.view.AnvilView
import kotlin.coroutines.resume

/**
 * Anvil input menu backed by Paper's real anvil view.
 */
public class AnvilMenu(
    public val title: String,
    public val placeholder: String = "",
) {
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    public suspend fun open(player: Player): String? =
        suspendCancellableCoroutine { continuation ->
            val anvilView = MenuType.ANVIL.create(player, title.mm())
            val inputItem = ItemStack(Material.PAPER)
            val meta = inputItem.itemMeta
            if (meta != null && placeholder.isNotEmpty()) {
                meta.displayName(placeholder.mm())
                inputItem.itemMeta = meta
            }

            val listener =
                object : Listener {
                    private var completed = false

                    @EventHandler
                    fun onClick(event: InventoryClickEvent) {
                        if (event.whoClicked.uniqueId != player.uniqueId) return
                        if (event.view != anvilView) return
                        if (event.rawSlot != 2) return

                        val result = readInput(anvilView)
                        completed = true
                        HandlerList.unregisterAll(this)
                        Bukkit.getScheduler().runTask(
                            DaisyMenu.plugin(),
                            Runnable {
                                if (player.openInventory.topInventory == anvilView.topInventory) {
                                    player.closeInventory()
                                }
                                if (continuation.isActive) {
                                    continuation.resume(result)
                                }
                            },
                        )
                    }

                    @EventHandler
                    fun onClose(event: InventoryCloseEvent) {
                        if (event.player.uniqueId != player.uniqueId) return
                        if (event.view != anvilView) return

                        HandlerList.unregisterAll(this)
                        if (completed) return

                        val result = readInput(anvilView)
                        Bukkit.getScheduler().runTask(
                            DaisyMenu.plugin(),
                            Runnable {
                                if (continuation.isActive) {
                                    continuation.resume(result)
                                }
                            },
                        )
                    }
                }

            continuation.invokeOnCancellation {
                HandlerList.unregisterAll(listener)
                Bukkit.getScheduler().runTask(
                    DaisyMenu.plugin(),
                    Runnable {
                        if (player.openInventory.topInventory == anvilView.topInventory) {
                            player.closeInventory()
                        }
                    },
                )
            }

            Bukkit.getPluginManager().registerEvents(listener, DaisyMenu.plugin())
            Bukkit.getScheduler().runTask(
                DaisyMenu.plugin(),
                Runnable {
                    player.openInventory(anvilView)
                    anvilView.topInventory.setItem(0, inputItem)
                },
            )
        }

    private fun readInput(view: AnvilView): String? =
        view.renameText?.takeIf { it.isNotBlank() }
            ?: view.topInventory
                .getItem(2)
                ?.itemMeta
                ?.displayName()
                ?.let { component -> plainSerializer.serialize(component).takeIf { it.isNotBlank() } }
}
