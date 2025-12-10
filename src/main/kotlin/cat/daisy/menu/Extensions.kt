package cat.daisy.menu

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player

/**
 * Get a coroutine dispatcher that executes on the Bukkit main thread.
 * This is the proper way to get a dispatcher for Paper 1.21.10+
 */
internal fun getBukkitDispatcher() = Dispatchers.Main.immediate

/**
 * Extension function for Player to open a menu.
 *
 * Usage:
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
 */
suspend fun Player.openMenu(block: MenuBuilder.() -> Unit) {
    val builder = MenuBuilder()
    builder.apply(block)
    val menu = builder.build()
    menu.open(this)
}

/**
 * Open anvil input menu (non-suspendable convenience).
 *
 * Usage:
 * ```kotlin
 * player.openAnvilAsync("Enter Name") { result ->
 *     if (result != null) {
 *         player.sendMessage("You entered: $result")
 *     }
 * }
 * ```
 */
fun Player.openAnvilAsync(
    title: String,
    block: (String?) -> Unit,
) {
    DaisyMenu.getScope().launch {
        val result = openAnvil(title)
        block(result)
    }
}

/**
 * Open anvil input menu (suspendable).
 *
 * Usage:
 * ```kotlin
 * val name = player.openAnvil("Enter Name")
 * if (name != null) {
 *     player.sendMessage("You entered: $name")
 * }
 * ```
 */
suspend fun Player.openAnvil(title: String): String? {
    val anvil = AnvilMenu(title)
    return anvil.open(this)
}

/**
 * Convenience: open anvil with placeholder.
 */
suspend fun Player.openAnvil(
    title: String,
    placeholder: String,
): String? {
    val anvil = AnvilMenu(title, placeholder)
    return anvil.open(this)
}
