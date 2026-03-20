package cat.daisy.menu.example

import cat.daisy.menu.DaisyMenu
import cat.daisy.menu.menu
import cat.daisy.menu.openMenu
import cat.daisy.menu.text.DaisyText.mm
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Example plugin demonstrating the DaisyMenu 2.0 API.
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
                template {
                    border {
                        name = " "
                    }
                }

                slot(11) {
                    item(Material.DIAMOND) {
                        name = "&bDiamond"
                        lore("&7Price: &a100 coins", "&8Click to buy")
                    }
                    onClick {
                        player.sendMessage("&aDiamond purchased".mm())
                    }
                }

                slot(13) {
                    render {
                        cat.daisy.menu.item(Material.CLOCK) {
                            name = "&fOnline: &b${Bukkit.getOnlinePlayers().size}"
                        }
                    }
                    refreshEvery(20)
                }

                slot(15) {
                    item(Material.BARRIER) {
                        name = "&cClose"
                    }
                    closeOnClick()
                }
            },
        )
    }

    private fun openPlayerList(player: Player) {
        val players = Bukkit.getOnlinePlayers().toList()
        player.openMenu(
            menu("&b&lPlayers", rows = 6) {
                template {
                    border {
                        name = " "
                    }
                    content(10..43)
                    navBar(6) {
                        previous()
                        pageLabel()
                        next()
                    }
                }

                pagination(itemsPerPage = 28) {
                    pageCount(players.size)

                    pageItems(players).forEachIndexed { index, target ->
                        val targetSlot =
                            listOf(
                                10,
                                11,
                                12,
                                13,
                                14,
                                15,
                                16,
                                19,
                                20,
                                21,
                                22,
                                23,
                                24,
                                25,
                                28,
                                29,
                                30,
                                31,
                                32,
                                33,
                                34,
                                37,
                                38,
                                39,
                                40,
                                41,
                                42,
                                43,
                            )[index]

                        slot(targetSlot) {
                            item(Material.PLAYER_HEAD) {
                                name = "&b${target.name}"
                                lore("&7Click to teleport")
                                skullOwner(target)
                            }
                            onClick {
                                player.teleport(target.location)
                            }
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
