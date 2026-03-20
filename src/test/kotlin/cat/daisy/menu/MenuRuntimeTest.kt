package cat.daisy.menu

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        val clickHandler: suspend MenuClickContext.() -> Unit = {
            events += "click"
        }
        var onOpenSawInventory = false

        val session =
            player.openMenu(
                menu("Lifecycle", rows = 1) {
                    slot(0) {
                        item(Material.STONE) {
                            name = "Click"
                        }
                        onClick(clickHandler)
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

        session.close()

        assertEquals(listOf("open", "click", "close"), events)
    }

    @Test
    fun `bottom inventory left click stays allowed but shift click is blocked`() {
        player.inventory.setItem(0, ItemStack(Material.STONE))
        player.openMenu(menu("Bottom", rows = 1) {})

        val normalClick = player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 9)
        val shiftClick = player.simulateInventoryClick(player.openInventory, ClickType.SHIFT_LEFT, 9)

        assertFalse(normalClick.isCancelled)
        assertTrue(shiftClick.isCancelled)
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
    fun `pagination buttons change page and clamp when data shrinks`() {
        val numbers = (1..25).toMutableList()
        val nextHandler: suspend MenuClickContext.() -> Unit = { nextPage() }

        val session =
            player.openMenu(
                menu("Pages", rows = 1) {
                    pagination(itemsPerPage = 10) {
                        val pageItems = pageItems(numbers)
                        pageCount(numbers.size)

                        slot(0) {
                            item(Material.PAPER) {
                                name = pageItems.firstOrNull()?.toString() ?: "Empty"
                            }
                        }

                        if (hasNext()) {
                            slot(8) {
                                item(Material.ARROW) {
                                    name = "Next"
                                }
                                onClick(nextHandler)
                            }
                        }
                    }
                },
            )

        assertEquals(
            "1",
            plain.serialize(
                assertNotNull(
                    session.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )

        player.simulateInventoryClick(player.openInventory, ClickType.LEFT, 8)
        assertEquals(1, session.currentPage)
        assertEquals(
            "11",
            plain.serialize(
                assertNotNull(
                    session.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )

        numbers.clear()
        numbers += 1..5
        session.invalidate()

        assertEquals(0, session.currentPage)
        assertEquals(
            "1",
            plain.serialize(
                assertNotNull(
                    session.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )
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

    @Test
    fun `dynamic slot refresh updates rendered items`() {
        var counter = 0
        val session =
            player.openMenu(
                menu("Dynamic", rows = 1) {
                    slot(0) {
                        render {
                            cat.daisy.menu.item(Material.PAPER) {
                                name = counter.toString()
                            }
                        }
                    }
                },
            )

        assertEquals(
            "0",
            plain.serialize(
                assertNotNull(
                    session.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )

        counter = 5
        session.invalidate(0)

        assertEquals(
            "5",
            plain.serialize(
                assertNotNull(
                    session.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )
    }
}
