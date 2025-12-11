package cat.daisy.menu

import cat.daisy.menu.text.DaisyText.mm
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
 * A button in a menu - represents an ItemStack with optional click handler.
 * Immutable once created for thread safety.
 */
public class Button(
    public val itemStack: ItemStack,
    internal val clickHandler: (suspend (Player, ClickType) -> Unit)? = null,
) {
    public companion object {
        /**
         * Create a button with no click handler (decoration).
         */
        public fun empty(): Button = Button(ItemStack(Material.AIR))

        /**
         * Create a simple decoration button.
         */
        public fun decoration(
            material: Material,
            name: String = " ",
        ): Button = Button(item(material) { name(name) })
    }

    /**
     * Check if this button has a click handler.
     */
    public fun hasClickHandler(): Boolean = clickHandler != null

    public suspend fun invokeClick(
        player: Player,
        clickType: ClickType,
    ) {
        clickHandler?.invoke(player, clickType)
    }
}

/**
 * DSL builder for creating ItemStacks with proper MiniMessage formatting.
 * Supports all modern Paper/Adventure features.
 */
public class ItemBuilder(
    private val material: Material,
) {
    private var name: Component? = null
    private var lore: MutableList<Component> = mutableListOf()
    private var amount: Int = 1
    private var customModelData: Int? = null
    private var glowing: Boolean = false
    private var unbreakable: Boolean = false
    private val flags: MutableSet<ItemFlag> = mutableSetOf()
    private val enchantments: MutableMap<Enchantment, Int> = mutableMapOf()
    private var skullOwner: UUID? = null
    private val persistentData: MutableMap<String, Any> = mutableMapOf()

    /**
     * Set the display name using MiniMessage formatting.
     */
    public fun name(text: String) {
        this.name = text.mm()
    }

    /**
     * Set the display name using a Component.
     */
    public fun name(component: Component) {
        this.name = component
    }

    /**
     * Set the lore using MiniMessage formatting.
     */
    public fun lore(vararg lines: String) {
        this.lore = lines.map { it.mm() }.toMutableList()
    }

    /**
     * Set the lore using a list of strings with MiniMessage formatting.
     */
    public fun lore(lines: List<String>) {
        this.lore = lines.map { it.mm() }.toMutableList()
    }

    /**
     * Set the lore using Components.
     */
    public fun loreComponents(lines: List<Component>) {
        this.lore = lines.toMutableList()
    }

    /**
     * Add a single lore line.
     */
    public fun addLore(line: String) {
        this.lore.add(line.mm())
    }

    /**
     * Set the stack amount.
     */
    public fun amount(count: Int): ItemBuilder =
        apply {
            require(count in 1..64) { "Amount must be between 1 and 64" }
            this.amount = count
        }

    /**
     * Set custom model data for resource pack textures.
     */
    public fun customModelData(data: Int): ItemBuilder = apply { this.customModelData = data }

    /**
     * Add enchantment glow effect without visible enchantment.
     */
    public fun glow(): ItemBuilder = apply { this.glowing = true }

    /**
     * Make the item unbreakable.
     */
    public fun unbreakable(): ItemBuilder = apply { this.unbreakable = true }

    /**
     * Add an enchantment.
     */
    public fun enchant(
        enchantment: Enchantment,
        level: Int = 1,
    ): ItemBuilder =
        apply {
            this.enchantments[enchantment] = level
        }

    /**
     * Add item flags to hide attributes.
     */
    public fun flags(vararg itemFlags: ItemFlag): ItemBuilder =
        apply {
            this.flags.addAll(itemFlags)
        }

    /**
     * Hide all item attributes (enchants, attributes, etc).
     */
    public fun hideAttributes(): ItemBuilder =
        apply {
            this.flags.addAll(ItemFlag.entries)
        }

    /**
     * Set skull owner for PLAYER_HEAD material.
     */
    public fun skullOwner(uuid: UUID): ItemBuilder =
        apply {
            this.skullOwner = uuid
        }

    /**
     * Set skull owner for PLAYER_HEAD material by player.
     */
    public fun skullOwner(player: Player): ItemBuilder =
        apply {
            this.skullOwner = player.uniqueId
        }

    /**
     * Store persistent data on the item (survives server restarts).
     */
    public fun persistentData(
        key: String,
        value: String,
    ): ItemBuilder =
        apply {
            this.persistentData[key] = value
        }

    /**
     * Store persistent data on the item (survives server restarts).
     */
    public fun persistentData(
        key: String,
        value: Int,
    ): ItemBuilder =
        apply {
            this.persistentData[key] = value
        }

    /**
     * Build the ItemStack with all configured properties.
     */
    public fun build(): ItemStack {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item

        // Display name (with italic disabled by default via mm())
        name?.let { meta.displayName(it) }

        // Lore
        if (lore.isNotEmpty()) {
            meta.lore(lore)
        }

        // Custom model data (suppress deprecation as this is the standard way pre-1.21.3)
        @Suppress("DEPRECATION")
        customModelData?.let { meta.setCustomModelData(it) }

        // Glow effect
        if (glowing) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        // Regular enchantments
        enchantments.forEach { (enchant, level) ->
            meta.addEnchant(enchant, level, true)
        }

        // Unbreakable
        if (unbreakable) {
            meta.isUnbreakable = true
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        }

        // Item flags
        if (flags.isNotEmpty()) {
            meta.addItemFlags(*flags.toTypedArray())
        }

        // Skull owner
        if (material == Material.PLAYER_HEAD && skullOwner != null && meta is SkullMeta) {
            meta.owningPlayer = org.bukkit.Bukkit.getOfflinePlayer(skullOwner!!)
        }

        // Persistent data
        if (persistentData.isNotEmpty()) {
            val container = meta.persistentDataContainer
            persistentData.forEach { (key, value) ->
                val namespacedKey = NamespacedKey(DaisyMenu.getPlugin(), key)
                when (value) {
                    is String -> container.set(namespacedKey, PersistentDataType.STRING, value)
                    is Int -> container.set(namespacedKey, PersistentDataType.INTEGER, value)
                }
            }
        }

        item.itemMeta = meta
        return item
    }
}

/**
 * Create an ItemStack with the DSL.
 */
public fun item(
    material: Material,
    block: ItemBuilder.() -> Unit = {},
): ItemStack = ItemBuilder(material).apply(block).build()

/**
 * Create a Button with an ItemStack and optional click handler.
 */
public fun button(
    material: Material,
    block: ItemBuilder.() -> Unit = {},
    onClick: (suspend (Player, ClickType) -> Unit)? = null,
): Button {
    val itemStack = item(material, block)
    return Button(itemStack, onClick)
}

/**
 * Create a Button from an existing ItemStack.
 */
public fun button(
    itemStack: ItemStack,
    onClick: (suspend (Player, ClickType) -> Unit)? = null,
): Button = Button(itemStack, onClick)
