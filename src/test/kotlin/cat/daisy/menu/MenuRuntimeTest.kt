package cat.daisy.menu

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MenuRuntimeTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin
    private lateinit var player: PlayerMock
    private val plain = PlainTextComponentSerializer.plainText()

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        DaisyMenu.initialize(plugin)
        player = server.addPlayer()
    }

    @AfterEach
    fun tearDown() {
        DaisyMenu.shutdown()
        MockBukkit.unmock()
    }

    @Test
    fun `open click and close lifecycle execute in order`() {
        val events = mutableListOf<String>()
        var onOpenSawInventory = false

        val session =
            player.openMenu(
                menu("Lifecycle", rows = 1) {
                    slot(0) {
                        item(Material.STONE) {
                            name = "Click"
                        }
                        onClick(
                            onMenuClick {
                                events += "click"
                            },
                        )
                    }
                    onOpen {
                        onOpenSawInventory = player.openInventory.topInventory == it.inventory
                        events += "open"
                    }
                    onClose {
                        events += "close"
                    }
                },
            )

        val event = player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 0)
        assertTrue(event.isCancelled)
        assertTrue(onOpenSawInventory)
        assertTrue(session.hasOpenInventory())

        session.close()

        assertEquals(listOf("open", "click", "close"), events)
        assertFalse(session.hasOpenInventory())
    }

    @Test
    fun `filtered click handlers run in declaration order and fallback remains available`() {
        val events = mutableListOf<String>()
        player.openMenu(
            menu("Clicks", rows = 1) {
                slot(0) {
                    item(Material.STONE) {
                        name = "Click"
                    }
                    onShiftClick {
                        events += "shift"
                    }
                    onLeftClick {
                        events += "left"
                    }
                    onClick(
                        onMenuClick {
                            events += "fallback"
                        },
                    )
                }
            },
        )

        player.simulateInventoryClick(player.openInventory, ClickType.SHIFT_LEFT, 0)
        player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 0)
        player.simulateInventoryClick(player.openInventory, ClickType.RIGHT, 0)

        assertEquals(listOf("shift", "left", "fallback"), events)
    }

    @Test
    fun `render null clears a slot and invalidate vararg updates multiple slots`() {
        var visible = true
        var clicks = 0
        val session =
            player.openMenu(
                menu("Dynamic", rows = 1) {
                    slot(0) {
                        render {
                            if (!visible) {
                                return@render null
                            }
                            cat.daisy.menu.item(Material.PAPER) {
                                name = "Visible"
                            }
                        }
                    }

                    slot(1) {
                        render {
                            cat.daisy.menu.item(Material.EMERALD) {
                                name = "Clicks: $clicks"
                            }
                        }
                    }

                    slot(8) {
                        item(Material.LEVER) {
                            name = "Toggle"
                        }
                        invalidateOnClick(0, 1)
                        onClick(
                            onMenuClick {
                                visible = !visible
                                clicks++
                            },
                        )
                    }
                },
            )

        assertNotNull(session.inventory.getItem(0))
        assertEquals("Clicks: 0", displayName(session.inventory.getItem(1)))

        player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 8)

        assertNull(session.inventory.getItem(0))
        assertEquals("Clicks: 1", displayName(session.inventory.getItem(1)))
    }

    @Test
    fun `bottom inventory normal click stays allowed while transfer actions are blocked`() {
        player.inventory.setItem(0, ItemStack(Material.STONE))
        player.openMenu(menu("Bottom", rows = 1) {})

        val normalClick = clickEvent(rawSlot = 9, clickType = ClickType.LEFT, action = InventoryAction.PICKUP_ALL)
        val shiftClick =
            clickEvent(
                rawSlot = 9,
                clickType = ClickType.SHIFT_LEFT,
                action = InventoryAction.MOVE_TO_OTHER_INVENTORY,
            )
        val collectClick =
            clickEvent(
                rawSlot = 9,
                clickType = ClickType.DOUBLE_CLICK,
                action = InventoryAction.COLLECT_TO_CURSOR,
            )
        val hotbarMove =
            clickEvent(
                rawSlot = 9,
                clickType = ClickType.NUMBER_KEY,
                action = InventoryAction.HOTBAR_MOVE_AND_READD,
                hotbarButton = 0,
            )

        assertFalse(normalClick.isCancelled)
        assertTrue(shiftClick.isCancelled)
        assertTrue(collectClick.isCancelled)
        assertTrue(hotbarMove.isCancelled)
    }

    @Test
    fun `top inventory number key swaps are cancelled`() {
        player.inventory.setItem(0, ItemStack(Material.DIAMOND))
        player.openMenu(
            menu("Top", rows = 1) {
                slot(0) {
                    item(Material.STONE) {
                        name = "Locked"
                    }
                }
            },
        )

        val event =
            clickEvent(
                rawSlot = 0,
                clickType = ClickType.NUMBER_KEY,
                action = InventoryAction.HOTBAR_SWAP,
                hotbarButton = 0,
            )

        assertTrue(event.isCancelled)
        assertEquals("Locked", displayName(player.openInventory.topInventory.getItem(0)))
    }

    @Test
    fun `dragging into the menu inventory is cancelled`() {
        player.openMenu(menu("Drag", rows = 1) {})

        val dragEvent =
            InventoryDragEvent(
                player.openInventory,
                ItemStack(Material.STONE),
                ItemStack(Material.AIR),
                false,
                mapOf(0 to ItemStack(Material.STONE)),
            )

        server.pluginManager.callEvent(dragEvent)

        assertTrue(dragEvent.isCancelled)
    }

    @Test
    fun `pagination helpers change page and update labels`() {
        val numbers = (1..9).toList()
        val session =
            player.openMenu(
                menu("Pages", rows = 2) {
                    template {
                        navBar(2) {
                            previous()
                            pageLabel()
                            next()
                        }
                    }

                    pagination(itemsPerPage = 3) {
                        val pageNumbers = pageItems(numbers)
                        pageCount(numbers.size)

                        pageNumbers.forEachIndexed { index, value ->
                            slot(index) {
                                item(Material.PAPER) {
                                    name = value.toString()
                                }
                            }
                        }
                    }
                },
            )

        assertEquals("1", displayName(session.inventory.getItem(0)))
        assertEquals("Page 1/3", displayName(session.inventory.getItem(13)))
        assertNull(session.inventory.getItem(9))
        assertNotNull(session.inventory.getItem(17))

        player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 17)

        assertEquals(1, session.currentPage)
        assertEquals("4", displayName(session.inventory.getItem(0)))
        assertEquals("Page 2/3", displayName(session.inventory.getItem(13)))
        assertNotNull(session.inventory.getItem(9))
    }

    @Test
    fun `pagination buttons clamp when data shrinks`() {
        val numbers = (1..25).toMutableList()

        val session =
            player.openMenu(
                menu("Clamp", rows = 1) {
                    pagination(itemsPerPage = 10) {
                        val pageItems = pageItems(numbers)
                        pageCount(numbers.size)

                        slot(0) {
                            item(Material.PAPER) {
                                name = pageItems.firstOrNull()?.toString() ?: "Empty"
                            }
                        }

                        nextButton(8)
                    }
                },
            )

        player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 8)
        assertEquals(1, session.currentPage)
        assertEquals("11", displayName(session.inventory.getItem(0)))

        numbers.clear()
        numbers += 1..5
        session.invalidate()

        assertEquals(0, session.currentPage)
        assertEquals("1", displayName(session.inventory.getItem(0)))
    }

    @Test
    fun `session refresh tasks stop after close`() {
        var executions = 0
        val session = player.openMenu(menu("Refresh", rows = 1) {})

        session.refreshEvery(1) {
            executions++
        }

        server.scheduler.performTicks(3)
        assertEquals(3, executions)

        session.close()
        server.scheduler.performTicks(3)

        assertEquals(3, executions)
    }

    private fun clickEvent(
        rawSlot: Int,
        clickType: ClickType,
        action: InventoryAction,
        hotbarButton: Int? = null,
    ): InventoryClickEvent {
        val view = player.openInventory
        val event =
            if (hotbarButton == null) {
                InventoryClickEvent(view, view.getSlotType(rawSlot), rawSlot, clickType, action)
            } else {
                InventoryClickEvent(view, view.getSlotType(rawSlot), rawSlot, clickType, action, hotbarButton)
            }
        server.pluginManager.callEvent(event)
        return event
    }

    private fun displayName(itemStack: ItemStack?): String =
        plain.serialize(
            assertNotNull(
                itemStack
                    ?.itemMeta
                    ?.displayName(),
            ),
        )
}
