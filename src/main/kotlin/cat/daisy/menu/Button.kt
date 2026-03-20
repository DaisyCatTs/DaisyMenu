package cat.daisy.menu

import cat.daisy.menu.text.DaisyText
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * Immutable static slot helper.
 */
public class Button internal constructor(
    internal val definition: SlotDefinition,
) {
    public val itemStack: ItemStack?
        get() = definition.previewItem()

    public companion object {
        public fun empty(): Button = Button(SlotDefinition())

        public fun decoration(
            material: Material,
            displayName: String = " ",
        ): Button =
            button(
                material = material,
                block = {
                    name(displayName)
                },
            )
    }
}

public class ItemBuilder(
    private val material: Material,
) {
    public var name: String? = null
    private var nameComponent: Component? = null
    private val loreLines = mutableListOf<Component>()
    private var stackAmount: Int = 1
    private var customModelData: Int? = null
    private var glowing: Boolean = false
    private var unbreakable: Boolean = false
    private val flags = mutableSetOf<ItemFlag>()
    private val enchantments = mutableMapOf<Enchantment, Int>()
    private var skullOwner: UUID? = null
    private val persistentData = mutableMapOf<String, Any>()

    public fun name(component: Component) {
        name = null
        nameComponent = component
    }

    public fun name(text: String) {
        name = text
        nameComponent = null
    }

    public fun lore(vararg lines: String) {
        loreLines.clear()
        loreLines += lines.map { DaisyText.run { it.mm() } }
    }

    public fun lore(lines: List<String>) {
        loreLines.clear()
        loreLines += lines.map { DaisyText.run { it.mm() } }
    }

    public fun loreComponents(lines: List<Component>) {
        loreLines.clear()
        loreLines += lines
    }

    public fun addLore(line: String) {
        loreLines += DaisyText.run { line.mm() }
    }

    public fun amount(count: Int): ItemBuilder =
        apply {
            require(count in 1..64) { "Amount must be between 1 and 64" }
            stackAmount = count
        }

    public fun customModelData(data: Int): ItemBuilder = apply { customModelData = data }

    public fun glow(): ItemBuilder = apply { glowing = true }

    public fun unbreakable(): ItemBuilder = apply { unbreakable = true }

    public fun enchant(
        enchantment: Enchantment,
        level: Int = 1,
    ): ItemBuilder =
        apply {
            enchantments[enchantment] = level
        }

    public fun flags(vararg itemFlags: ItemFlag): ItemBuilder =
        apply {
            flags.addAll(itemFlags)
        }

    public fun hideAttributes(): ItemBuilder =
        apply {
            flags.addAll(ItemFlag.entries)
        }

    public fun skullOwner(uuid: UUID): ItemBuilder =
        apply {
            skullOwner = uuid
        }

    public fun skullOwner(player: Player): ItemBuilder =
        apply {
            skullOwner = player.uniqueId
        }

    public fun persistentData(
        key: String,
        value: String,
    ): ItemBuilder =
        apply {
            persistentData[key] = value
        }

    public fun persistentData(
        key: String,
        value: Int,
    ): ItemBuilder =
        apply {
            persistentData[key] = value
        }

    public fun build(): ItemStack {
        val itemStack = ItemStack(material, stackAmount)
        val meta = itemStack.itemMeta ?: return itemStack

        val displayName = nameComponent ?: name?.let { DaisyText.run { it.mm() } }
        displayName?.let(meta::displayName)

        if (loreLines.isNotEmpty()) {
            meta.lore(loreLines.toList())
        }

        @Suppress("DEPRECATION")
        customModelData?.let(meta::setCustomModelData)

        if (glowing) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        enchantments.forEach { (enchantment, level) ->
            meta.addEnchant(enchantment, level, true)
        }

        if (unbreakable) {
            meta.isUnbreakable = true
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        }

        if (flags.isNotEmpty()) {
            meta.addItemFlags(*flags.toTypedArray())
        }

        if (material == Material.PLAYER_HEAD && skullOwner != null && meta is SkullMeta) {
            meta.owningPlayer = org.bukkit.Bukkit.getOfflinePlayer(skullOwner!!)
        }

        if (persistentData.isNotEmpty()) {
            val container = meta.persistentDataContainer
            persistentData.forEach { (key, value) ->
                val namespacedKey = NamespacedKey(DaisyMenu.plugin(), key)
                when (value) {
                    is String -> container.set(namespacedKey, PersistentDataType.STRING, value)
                    is Int -> container.set(namespacedKey, PersistentDataType.INTEGER, value)
                }
            }
        }

        itemStack.itemMeta = meta
        return itemStack
    }
}

public fun item(
    material: Material,
    block: ItemBuilder.() -> Unit = {},
): ItemStack = ItemBuilder(material).apply(block).build()

public fun button(
    material: Material,
    block: ItemBuilder.() -> Unit = {},
    onClick: MenuClickAction? = null,
): Button = button(item(material, block), onClick)

public fun button(
    itemStack: ItemStack,
    onClick: MenuClickAction? = null,
): Button =
    Button(
        SlotDefinition(
            item = itemStack,
            clickBindings =
                onClick?.let { action ->
                    listOf(MenuClickBinding({ true }, action))
                } ?: emptyList(),
        ),
    )

public fun button(
    material: Material,
    block: ItemBuilder.() -> Unit = {},
    onClick: suspend MenuClickContext.() -> Unit,
): Button = button(item(material, block), onMenuClick(onClick))

public fun button(
    itemStack: ItemStack,
    onClick: suspend MenuClickContext.() -> Unit,
): Button = button(itemStack, onMenuClick(onClick))

public fun button(
    material: Material,
    block: ItemBuilder.() -> Unit = {},
    onClick: suspend (Player, ClickType) -> Unit,
): Button =
    button(
        material = material,
        block = block,
        onClick =
            onMenuClick {
                onClick(player, clickType)
            },
    )

public fun button(
    itemStack: ItemStack,
    onClick: suspend (Player, ClickType) -> Unit,
): Button =
    button(
        itemStack = itemStack,
        onClick =
            onMenuClick {
                onClick(player, clickType)
            },
    )
