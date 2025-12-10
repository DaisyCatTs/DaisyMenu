<p align="center">
  <img src="https://img.shields.io/badge/DaisyMenu-v1.0.0-ff69b4?style=for-the-badge&logo=kotlin&logoColor=white" alt="DaisyMenu v1.0.0"/>
</p>

<h1 align="center">🌸 DaisyMenu</h1>

<h3 align="center">The #1 Kotlin GUI Library for Paper Minecraft Servers</h3>

<p align="center">
  <a href="https://jitpack.io/#fu3i0n/DaisyMenu"><img src="https://jitpack.io/v/fu3i0n/DaisyMenu.svg" alt="JitPack"/></a>
  <a href="https://github.com/fu3i0n/DaisyMenu/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="MIT License"/></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg?logo=kotlin" alt="Kotlin"/></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper-1.21+-4CAF50.svg" alt="Paper 1.21+"/></a>
  <a href="https://openjdk.org"><img src="https://img.shields.io/badge/Java-21+-ED8B00.svg?logo=openjdk" alt="Java 21+"/></a>
</p>

<p align="center">
  <strong>✨ Beautiful DSL • Coroutine-First • Dupe-Safe • Production-Ready ✨</strong>
</p>

<p align="center">
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-features">Features</a> •
  <a href="#-installation">Installation</a> •
  <a href="#-api-reference">API Reference</a> •
  <a href="#-examples">Examples</a>
</p>

---

## 🎯 Why DaisyMenu?

> **Write beautiful GUIs in 6 lines, not 60.**

DaisyMenu is a **modern, coroutine-first GUI library** built specifically for Paper and its forks. It provides the cleanest DSL syntax, complete dupe protection, and seamless async support.

```kotlin
player.openMenu {
    title = "&6&l✦ Shop ✦"
    rows = 3
    
    slot(13) {
        item(Material.DIAMOND) { 
            name("&b💎 Diamond")
            lore("&7Price: &a$100", "&8Click to buy")
            glow()
        }
        onClick { player, _ -> 
            player.sendMessage("&a✓ Purchased!".mm())
        }
    }
}
```

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🎨 **Beautiful DSL** | Clean, readable Kotlin syntax that feels natural |
| ⚡ **Coroutine-First** | Full `suspend` function support in click handlers |
| 🔒 **Dupe Protection** | Complete protection against all known item duplication exploits |
| 📝 **MiniMessage** | Built-in text formatting with gradients, hex colors, and legacy codes |
| 📄 **Pagination** | Easy multi-page menus with automatic navigation |
| ✏️ **Anvil Input** | Get text input with a simple `suspend` function call |
| 🎯 **Pattern System** | Create complex layouts with string-based patterns |
| 🔄 **Live Updates** | Update menu items in real-time without rebuilding |
| 🎭 **Player Heads** | Easy skull textures with UUID or Player |
| 💾 **Persistent Data** | Store custom data on items using PDC |
| 🚀 **Paper Optimized** | Built specifically for Paper 1.21+ and its forks |
| ☕ **Java Compatible** | Works with Java plugins too |

---

## 📦 Installation

### Gradle (Kotlin DSL) — Recommended

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.fu3i0n:DaisyMenu:1.0.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.fu3i0n:DaisyMenu:1.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.fu3i0n</groupId>
    <artifactId>DaisyMenu</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Required Dependencies

DaisyMenu requires these dependencies (provided by Paper):
- **Paper API 1.21+**
- **Kotlin Stdlib** (for Kotlin plugins)
- **Kotlinx Coroutines**

---

## 🚀 Quick Start

### 1. Initialize DaisyMenu

```kotlin
import cat.daisy.menu.DaisyMenu

class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        // Initialize DaisyMenu - REQUIRED
        DaisyMenu.initialize(this)
    }
    
    override fun onDisable() {
        // Clean shutdown
        DaisyMenu.shutdown()
    }
}
```

### 2. Create Your First Menu

```kotlin
import cat.daisy.menu.openMenu
import cat.daisy.menu.text.DaisyText.mm

suspend fun openShop(player: Player) {
    player.openMenu {
        title = "&b&lShop"
        rows = 3
        
        // Fill background
        fill(Material.GRAY_STAINED_GLASS_PANE) { name(" ") }
        
        // Add items
        slot(11) {
            item(Material.DIAMOND) {
                name("&bDiamond")
                lore("&7Price: &a$100", "&8Click to purchase")
            }
            onClick { p, _ ->
                p.sendMessage("&a✓ Purchased!".mm())
            }
        }
        
        slot(15) {
            item(Material.BARRIER) { name("&cClose") }
            onClick { p, _ -> p.closeInventory() }
        }
    }
}
```

### 3. Open Menu from Command

```kotlin
getCommand("shop")?.setExecutor { sender, _, _, _ ->
    if (sender is Player) {
        // Use your coroutine scope
        scope.launch {
            openShop(sender)
        }
    }
    true
}
```

---

## 📚 API Reference

### Player Extensions

```kotlin
// Open a menu (suspend function)
suspend fun Player.openMenu(block: MenuBuilder.() -> Unit)

// Open anvil for text input (suspend function)
suspend fun Player.openAnvil(title: String, placeholder: String = ""): String?

// Open anvil with callback (non-suspend)
fun Player.openAnvilAsync(title: String, block: (String?) -> Unit)
```

### MenuBuilder

| Property/Method | Description |
|-----------------|-------------|
| `title: String` | Menu title with MiniMessage support |
| `rows: Int` | Number of rows (1-6, default: 3) |
| `slot(index) { }` | Define a button at slot index (0-53) |
| `slot(x, y) { }` | Define a button at x,y coordinates (1-indexed) |
| `fill { }` | Fill empty slots with default glass pane |
| `fill(Material) { }` | Fill empty slots with specific material |
| `fillBorder { }` | Fill only the border slots |
| `fillRow(row) { }` | Fill a specific row (1-indexed) |
| `fillColumn(col) { }` | Fill a specific column (1-indexed) |
| `fillSlots(1, 2, 3) { }` | Fill specific slot indices |
| `pattern(...) { }` | Apply a string-based pattern |
| `pagination(perPage) { }` | Enable pagination |
| `onOpen { menu -> }` | Callback when menu opens |
| `onClose { menu -> }` | Callback when menu closes |

### SlotBuilder

| Method | Description |
|--------|-------------|
| `item(Material) { }` | Set the item with builder |
| `item(ItemStack)` | Set the item directly |
| `onClick { player, clickType -> }` | Suspend click handler |
| `onClick { player -> }` | Suspend click handler (ignore click type) |
| `onClickSync { player, clickType -> }` | Non-suspend click handler |
| `onClickSync { player -> }` | Non-suspend click handler (ignore click type) |

### ItemBuilder

| Method | Description |
|--------|-------------|
| `name("text")` | Set display name (MiniMessage) |
| `name(Component)` | Set display name (Component) |
| `lore("line1", "line2")` | Set lore lines (MiniMessage) |
| `lore(listOf("..."))` | Set lore from list |
| `addLore("line")` | Add single lore line |
| `amount(count)` | Set stack size (1-64) |
| `glow()` | Add enchantment glow |
| `enchant(Enchantment, level)` | Add enchantment |
| `unbreakable()` | Make item unbreakable |
| `customModelData(id)` | Set custom model data |
| `flags(ItemFlag...)` | Add item flags |
| `hideAttributes()` | Hide all attributes |
| `skullOwner(UUID)` | Set skull owner |
| `skullOwner(Player)` | Set skull owner from player |
| `persistentData(key, value)` | Store PDC data |

### Menu (Runtime)

| Method | Description |
|--------|-------------|
| `updateSlot(index) { }` | Update a slot's button |
| `updateSlot(index, Button)` | Update a slot with button |
| `fill(Button)` | Fill empty slots at runtime |
| `repeatUpdate(ticks) { }` | Repeat task while menu is open |
| `close()` | Close the menu |

### Text Formatting

```kotlin
import cat.daisy.menu.text.DaisyText.mm

// MiniMessage parsing (italic disabled by default)
val component = "&cRed &lBold Text".mm()

// Legacy color codes
"&a&lGreen Bold"           // Works!
"&7Gray &8Dark Gray"       // Works!

// MiniMessage tags  
"<red>Red</red>"           // Works!
"<#FF69B4>Pink Hex"        // Works!
"<gradient:#FF0000:#0000FF>Gradient</gradient>"  // Works!

// Extension functions
"Rainbow Text".rainbow()                    // Rainbow gradient
"Gradient".gradient("#FF0000", "#0000FF")   // Custom gradient
```

---

## 🎨 Pattern System

Create complex layouts easily with string patterns:

```kotlin
player.openMenu {
    title = "&b&lPattern Demo"
    rows = 5
    
    pattern(
        "XXXXXXXXX",
        "X       X",
        "X   D   X",
        "X       X",
        "XXXXXXXXX"
    ) {
        'X' to { 
            item(Material.BLACK_STAINED_GLASS_PANE) { name(" ") }
        }
        'D' to {
            item(Material.DIAMOND) { 
                name("&bDiamond")
                glow()
            }
            onClick { p, _ -> p.sendMessage("Clicked!".mm()) }
        }
    }
}
```

---

## 📄 Pagination

Multi-page menus made simple:

```kotlin
suspend fun openPlayerList(player: Player) {
    val allPlayers = Bukkit.getOnlinePlayers().toList()
    
    player.openMenu {
        title = "&b&l👥 Online Players"
        rows = 6
        
        fill { name(" ") }
        
        pagination(itemsPerPage = 45) {
            totalPages((allPlayers.size + 44) / 45)
            
            for (i in pageItems()) {
                val target = allPlayers.getOrNull(i) ?: continue
                
                slot(i % 45) {
                    item(Material.PLAYER_HEAD) {
                        name("&6${target.name}")
                        lore("&7Click to teleport")
                        skullOwner(target)
                    }
                    onClick { viewer, _ ->
                        viewer.teleport(target.location)
                    }
                }
            }
            
            // Previous page button
            if (hasPrevious()) {
                slot(45) {
                    item(Material.ARROW) { name("&c◀ Previous") }
                    onClick { _, _ -> prevPage() }
                }
            }
            
            // Page indicator
            slot(49) {
                item(Material.PAPER) {
                    name("&fPage ${currentPage + 1}/$totalPages")
                }
            }
            
            // Next page button
            if (hasNext()) {
                slot(53) {
                    item(Material.ARROW) { name("&a▶ Next") }
                    onClick { _, _ -> nextPage() }
                }
            }
        }
    }
}
```

---

## ✏️ Anvil Text Input

Get text input with a simple suspend call:

```kotlin
suspend fun renameItem(player: Player) {
    val newName = player.openAnvil(
        title = "&e&lRename Item",
        placeholder = "&7Type new name..."
    )
    
    if (newName != null) {
        player.sendMessage("&a✓ Renamed to: &b$newName".mm())
    } else {
        player.sendMessage("&cCancelled".mm())
    }
}
```

---

## 🔄 Live Updates

Update menu content in real-time:

```kotlin
player.openMenu {
    title = "&b&lServer Status"
    rows = 3
    
    slot(13) {
        item(Material.CLOCK) { name("&7Loading...") }
    }
    
    onOpen { menu ->
        // Update every second (20 ticks)
        menu.repeatUpdate(20L) {
            val tps = Bukkit.getTPS()[0]
            val color = if (tps >= 18.0) "&a" else if (tps >= 15.0) "&e" else "&c"
            
            menu.updateSlot(13) {
                item(Material.CLOCK) {
                    name("$color⚡ TPS: %.1f".format(tps))
                    lore(
                        "&7Players: &b${Bukkit.getOnlinePlayers().size}",
                        "&7Memory: &b${Runtime.getRuntime().freeMemory() / 1024 / 1024}MB free"
                    )
                }
            }
        }
    }
}
```

---

## 🔒 Security Features

DaisyMenu includes **complete dupe protection** out of the box:

- ✅ Blocks all dangerous inventory actions (COLLECT_TO_CURSOR, HOTBAR_SWAP, etc.)
- ✅ Cancels shift-click exploits
- ✅ Prevents drag events on menu slots
- ✅ Clears cursor on close to prevent ghost items
- ✅ Validates clicker is the menu owner
- ✅ High-priority event handlers to override other plugins

**No configuration needed** — protection is automatic.

---

## ☕ Java Usage

DaisyMenu works with Java too:

```java
import cat.daisy.menu.DaisyMenu;
import cat.daisy.menu.MenuBuilder;
import cat.daisy.menu.ExtensionsKt;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        DaisyMenu.INSTANCE.initialize(this, null);
    }
    
    public void openMenu(Player player) {
        // Use the Java-friendly API
        MenuBuilder builder = new MenuBuilder();
        builder.setTitle("&bShop");
        builder.setRows(3);
        
        builder.slot(13, slot -> {
            slot.item(Material.DIAMOND, item -> {
                item.name("&bDiamond");
                item.lore("&7Click to buy");
                return null;
            });
            slot.onClickSync(p -> {
                p.sendMessage("Purchased!");
                return null;
            });
            return null;
        });
        
        // Build and open
        builder.build().open(player);
    }
}
```

---

## 🆚 Comparison

| Feature | DaisyMenu | mc-chestui-plus | InventoryFramework |
|---------|-----------|-----------------|-------------------|
| **DSL Quality** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Coroutine Support** | ✅ Native | ❌ | ❌ |
| **Dupe Protection** | ✅ Complete | ⚠️ Partial | ⚠️ Partial |
| **Pattern System** | ✅ | ❌ | ✅ |
| **Anvil Input** | ✅ Suspend | ❌ | ✅ Callback |
| **Live Updates** | ✅ | ❌ | ✅ |
| **Text Formatting** | ✅ Built-in | ❌ | ❌ |
| **Memory Efficiency** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Lines of Code** | ~6 | ~15 | ~20 |
| **Paper Optimized** | ✅ | ⚠️ | ⚠️ |

---

## 📁 Project Structure

```
cat.daisy.menu/
├── DaisyMenu.kt       # Main singleton, initialization
├── Menu.kt            # Menu class, runtime operations
├── MenuBuilder.kt     # DSL builder for menus
├── Button.kt          # Button class, ItemBuilder DSL
├── Pagination.kt      # Pagination handler
├── AnvilMenu.kt       # Anvil text input
├── Extensions.kt      # Player extension functions
└── text/
    └── DaisyText.kt   # MiniMessage text utilities
```

---

## 🛠️ Requirements

- **Paper 1.21+** (or forks like Purpur, Pufferfish)
- **Java 21+**
- **Kotlin 2.0+** (for Kotlin plugins)
- **Kotlinx Coroutines 1.8+**

---

## 📝 License

MIT License - see [LICENSE](LICENSE) for details.

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## 💬 Support

- **Issues**: [GitHub Issues](https://github.com/fu3i0n/DaisyMenu/issues)
- **Discussions**: [GitHub Discussions](https://github.com/fu3i0n/DaisyMenu/discussions)

---

<p align="center">
  Made with 💜 by <a href="https://github.com/fu3i0n">fu3i0n</a>
</p>

<p align="center">
  <sub>If DaisyMenu helps your project, consider giving it a ⭐!</sub>
</p>

