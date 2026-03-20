package cat.daisy.menu.example

import cat.daisy.menu.DaisyMenu
import cat.daisy.menu.MenuClickContext
import cat.daisy.menu.menu
import cat.daisy.menu.openMenu
import cat.daisy.menu.text.DaisyText.mm
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Example plugin demonstrating the current DaisyMenu API.
 */
internal class DaisyMenuExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        DaisyMenu.initialize(this)

        getCommand("shop")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                openShopMenu(sender)
            }
            true
        }

        getCommand("players")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                openPlayerList(sender)
            }
            true
        }
    }

    private fun openShopMenu(player: Player) {
        player.openMenu(
            menu("&b&lShop", rows = 3) {
                fill { name = " " }

                slot(11) {
                    item(Material.DIAMOND) {
                        name = "&bDiamond"
                        lore("&7Price: 100 coins", "&8Click to buy")
                    }
                    onClick { _: Player, _: org.bukkit.event.inventory.ClickType ->
                        player.sendMessage("&aDiamond purchased".mm())
                    }
                }

                slot(15) {
                    val closeHandler: suspend MenuClickContext.() -> Unit = {
                        close()
                    }
                    item(Material.BARRIER) {
                        name = "&cClose"
                    }
                    onClick(closeHandler)
                }
            },
        )
    }

    private fun openPlayerList(player: Player) {
        val players = Bukkit.getOnlinePlayers().toList()
        player.openMenu(
            menu("&b&lPlayers", rows = 6) {
                fill { name = " " }

                pagination(itemsPerPage = 45) {
                    val pagePlayers = pageItems(players)
                    pageCount(players.size)

                    pagePlayers.forEachIndexed { index, target ->
                        slot(index) {
                            item(Material.PLAYER_HEAD) {
                                name = "&b${target.name}"
                                lore("&7Click to teleport")
                                skullOwner(target)
                            }
                            onClick { _: Player, _: org.bukkit.event.inventory.ClickType ->
                                player.teleport(target.location)
                            }
                        }
                    }

                    if (hasPrevious()) {
                        slot(45) {
                            val previousHandler: suspend MenuClickContext.() -> Unit = {
                                previousPage()
                            }
                            item(Material.ARROW) { name = "&cPrevious" }
                            onClick(previousHandler)
                        }
                    }

                    if (hasNext()) {
                        slot(53) {
                            val nextHandler: suspend MenuClickContext.() -> Unit = {
                                nextPage()
                            }
                            item(Material.ARROW) { name = "&aNext" }
                            onClick(nextHandler)
                        }
                    }
                }
            },
        )
    }

    override fun onDisable() {
        DaisyMenu.shutdown()
    }
}
