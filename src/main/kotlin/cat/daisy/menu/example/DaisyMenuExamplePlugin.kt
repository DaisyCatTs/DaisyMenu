// Example plugin demonstrating DaisyMenu usage.
// This example shows:
// - Basic menu creation
// - Paginated player list
// - Anvil input
// - Live updating with coroutines

package cat.daisy.menu.example

import cat.daisy.menu.DaisyMenu
import cat.daisy.menu.getBukkitDispatcher
import cat.daisy.menu.openAnvil
import cat.daisy.menu.openMenu
import cat.daisy.menu.text.DaisyText.mm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class DaisyMenuExamplePlugin : JavaPlugin() {
    private lateinit var scope: CoroutineScope

    override fun onEnable() {
        // Initialize DaisyMenu
        scope = CoroutineScope(getBukkitDispatcher())
        DaisyMenu.initialize(this, scope)

        // Register commands
        getCommand("shop")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                scope.launch {
                    openShopMenu(sender)
                }
            }
            true
        }

        getCommand("rename")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                scope.launch {
                    renameItem(sender)
                }
            }
            true
        }

        getCommand("players")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                scope.launch {
                    openPlayerList(sender)
                }
            }
            true
        }

        getCommand("status")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                scope.launch {
                    openServerStatus(sender)
                }
            }
            true
        }

        logger.info("DaisyMenu Example Plugin enabled!")
    }

    private suspend fun openShopMenu(player: Player) {
        player.openMenu {
            title = "&b&lShop"
            rows = 6

            // Fill background
            fill {
                name(" ")
            }

            // Diamond
            slot(11) {
                item(Material.DIAMOND) {
                    name("&bDiamond")
                    lore("&7Price: 100 coins", "&8Click to buy")
                }
                onClick { p ->
                    p.sendMessage("&a✓ Diamond purchased!".mm())
                }
            }

            // Gold Ingot
            slot(13) {
                item(Material.GOLD_INGOT) {
                    name("&6Gold Ingot")
                    lore("&7Price: 50 coins", "&8Click to buy")
                }
                onClick { p ->
                    p.sendMessage("&a✓ Gold ingot purchased!".mm())
                }
            }

            // Emerald
            slot(15) {
                item(Material.EMERALD) {
                    name("&2Emerald")
                    lore("&7Price: 75 coins", "&8Click to buy")
                }
                onClick { p ->
                    p.sendMessage("&a✓ Emerald purchased!".mm())
                }
            }

            slot(49) {
                item(Material.BARRIER) {
                    name("&cClose")
                }
                onClick { p -> p.closeInventory() }
            }

            onClose {
                player.sendMessage("&8Shop closed".mm())
            }
        }
    }

    private suspend fun renameItem(player: Player) {
        val name = player.openAnvil("&e&lRename Item", "&7Type new name")
        if (name != null && name.isNotBlank()) {
            player.sendMessage("&a✓ Item renamed to: $name".mm())
        } else {
            player.sendMessage("&cCancelled".mm())
        }
    }

    private suspend fun openPlayerList(player: Player) {
        player.openMenu {
            title = "&b&lOnline Players"
            rows = 6

            val players = Bukkit.getOnlinePlayers().toList()
            val itemsPerPage = 45
            val pages = (players.size + itemsPerPage - 1) / itemsPerPage

            pagination(itemsPerPage = itemsPerPage) {
                totalPages(pages)

                // Fill page content
                val pageStart = currentPage * itemsPerPage
                val pageEnd = minOf(pageStart + itemsPerPage, players.size)
                val pageItems = players.subList(pageStart, pageEnd)

                pageItems.forEachIndexed { index, p ->
                    slot(index) {
                        item(Material.PLAYER_HEAD) {
                            name("&b${p.name}")
                            lore("&7Health: &c${p.health.toInt()}&7/20", "&7Level: &a${p.level}")
                        }
                        onClick { viewer ->
                            viewer.teleport(p.location)
                            viewer.sendMessage("&a✓ Teleported to ${p.name}".mm())
                        }
                    }
                }

                // Navigation
                if (hasPrevious()) {
                    slot(45) {
                        item(Material.ARROW) {
                            name("&c◀ Previous")
                        }
                        onClick { _ ->
                            prevPage()
                        }
                    }
                }

                if (hasNext()) {
                    slot(53) {
                        item(Material.ARROW) {
                            name("&a▶ Next")
                        }
                        onClick { _ ->
                            nextPage()
                        }
                    }
                }
            }

            onClose {
                player.sendMessage("&8Player list closed".mm())
            }
        }
    }

    private suspend fun openServerStatus(player: Player) {
        player.openMenu {
            title = "&b&lServer Status"
            rows = 3

            fill { name(" ") }

            slot(11) {
                item(Material.CLOCK) {
                    name("&aTPS")
                    lore("&7Loading...")
                }
            }

            slot(13) {
                item(Material.PLAYER_HEAD) {
                    name("&bPlayers")
                    lore("&7Loading...")
                }
            }

            slot(15) {
                item(Material.DIAMOND) {
                    name("&6Performance")
                    lore("&7Loading...")
                }
            }

            onOpen { menu ->
                // Static display for now - dynamic updates can be added later
                // when needed with proper scope handling
            }
        }
    }

    override fun onDisable() {
        DaisyMenu.shutdown()
        logger.info("DaisyMenu Example Plugin disabled!")
    }
}
