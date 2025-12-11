package cat.daisy.menu

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.coroutines.CoroutineContext

/**
 * A coroutine dispatcher that executes on the Bukkit main thread.
 * This is the proper way to run coroutines on the main thread in Minecraft.
 */
public class BukkitDispatcher : CoroutineDispatcher() {
    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            Bukkit.getScheduler().runTask(DaisyMenu.getPlugin(), block)
        }
    }
}

/**
 * Get a coroutine dispatcher that executes on the Bukkit main thread.
 * Use this instead of Dispatchers.Main which doesn't exist in Minecraft.
 */
public fun getBukkitDispatcher(): CoroutineDispatcher = BukkitDispatcher()

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
public suspend fun Player.openMenu(block: MenuBuilder.() -> Unit) {
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
public fun Player.openAnvilAsync(
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
public suspend fun Player.openAnvil(title: String): String? {
    val anvil = AnvilMenu(title)
    return anvil.open(this)
}

/**
 * Convenience: open anvil with placeholder.
 */
public suspend fun Player.openAnvil(
    title: String,
    placeholder: String,
): String? {
    val anvil = AnvilMenu(title, placeholder)
    return anvil.open(this)
}
