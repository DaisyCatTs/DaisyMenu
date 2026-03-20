package cat.daisy.menu

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class MenuBuilderTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin
    private val plain = PlainTextComponentSerializer.plainText()

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        DaisyMenu.initialize(plugin)
    }

    @AfterEach
    fun tearDown() {
        DaisyMenu.shutdown()
        MockBukkit.unmock()
    }

    @Test
    fun `blank titles are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            menu("   ") {}
        }
    }

    @Test
    fun `row count must be inside chest bounds`() {
        assertFailsWith<IllegalArgumentException> {
            menu("Broken", rows = 0) {}
        }
        assertFailsWith<IllegalArgumentException> {
            menu("Broken", rows = 7) {}
        }
    }

    @Test
    fun `pattern size is validated`() {
        assertFailsWith<IllegalArgumentException> {
            menu("Pattern", rows = 1) {
                pattern("1234567890") {}
            }
        }
        assertFailsWith<IllegalArgumentException> {
            menu("Pattern", rows = 1) {
                pattern("123", "456") {}
            }
        }
    }

    @Test
    fun `slot refresh requires dynamic renderer`() {
        assertFailsWith<IllegalArgumentException> {
            menu("Refresh", rows = 1) {
                slot(0) {
                    item = ItemStack(Material.STONE)
                    refreshEvery(20)
                }
            }
        }
    }

    @Test
    fun `template content validates reserved slots`() {
        assertFailsWith<IllegalArgumentException> {
            menu("Template", rows = 1) {
                template {
                    border()
                    content(0..10)
                }
            }
        }
    }

    @Test
    fun `menu definition can be opened for multiple players independently`() {
        val menu =
            menu("Shared", rows = 1) {
                slot(0) {
                    item =
                        cat.daisy.menu.item(Material.STONE) {
                            name = "Shared"
                        }
                }
            }

        val first = server.addPlayer("first")
        val second = server.addPlayer("second")

        val firstSession = first.openMenu(menu)
        val secondSession = second.openMenu(menu)

        assertNotSame(firstSession, secondSession)
        assertNotSame(firstSession.inventory, secondSession.inventory)
        assertEquals(
            "Shared",
            plain.serialize(
                assertNotNull(
                    firstSession.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )
        assertEquals(
            "Shared",
            plain.serialize(
                assertNotNull(
                    secondSession.inventory
                        .getItem(0)
                        ?.itemMeta
                        ?.displayName(),
                ),
            ),
        )
    }

    @Test
    fun `static items are cloned when menus are built and rendered`() {
        val original =
            cat.daisy.menu.item(Material.STONE) {
                name = "Original"
            }

        val menu =
            menu("Clone", rows = 1) {
                slot(0) {
                    item = original
                }
            }

        val meta = original.itemMeta
        meta.displayName(Component.text("Mutated"))
        original.itemMeta = meta

        val player = server.addPlayer()
        val session = player.openMenu(menu)
        val rendered = assertNotNull(session.inventory.getItem(0))

        assertEquals("Original", plain.serialize(assertNotNull(rendered.itemMeta.displayName())))
    }
}
