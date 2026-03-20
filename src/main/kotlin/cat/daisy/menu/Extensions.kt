package cat.daisy.menu

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine dispatcher that always resumes on the Bukkit main thread.
 */
public class BukkitDispatcher : CoroutineDispatcher() {
    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            Bukkit.getScheduler().runTask(DaisyMenu.plugin(), block)
        }
    }
}

public fun getBukkitDispatcher(): CoroutineDispatcher = BukkitDispatcher()

public fun Player.openMenu(menu: Menu): MenuSession = menu.open(this)

public fun Player.openMenu(
    title: String,
    rows: Int = 3,
    block: MenuBuilder.() -> Unit,
): MenuSession = openMenu(menu(title, rows, block))

public fun Player.openAnvilAsync(
    title: String,
    block: (String?) -> Unit,
) {
    DaisyMenu.scope().launch {
        block(openAnvil(title))
    }
}

public suspend fun Player.openAnvil(title: String): String? = AnvilMenu(title).open(this)

public suspend fun Player.openAnvil(
    title: String,
    placeholder: String,
): String? = AnvilMenu(title, placeholder).open(this)
