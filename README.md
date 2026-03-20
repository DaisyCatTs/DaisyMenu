# DaisyMenu

DaisyMenu is a Kotlin-first menu library for Paper.

It focuses on a small, clean API:

- immutable menu definitions
- per-viewer menu sessions
- safe top-inventory handling
- dynamic slot rendering
- suspendable click and lifecycle hooks
- pagination helpers that stay inside the DSL

## Features

- `menu(title, rows) { ... }` builder for reusable menu definitions
- `Player.openMenu(menu)` and `Player.openMenu(title, rows) { ... }`
- `slot`, `fill`, `fillRow`, `fillColumn`, `fillBorder`, and `pattern`
- `MenuSession` runtime API with `close()`, `invalidate()`, and `refreshEvery(...)`
- dynamic slots via `render { ... }`
- pagination via `pagination(itemsPerPage) { ... }`
- `onOpen` and `onClose` lifecycle callbacks
- anvil text input utilities
- Paper/Adventure component titles and MiniMessage-backed item text

## Installation

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.fu3i0n:DaisyMenu:1.1.0")
}
```

### Gradle Groovy

```groovy
repositories {
    mavenCentral()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.fu3i0n:DaisyMenu:1.1.0"
}
```

## Setup

Initialize DaisyMenu once in `onEnable()` and shut it down in `onDisable()`.

```kotlin
import cat.daisy.menu.DaisyMenu
import org.bukkit.plugin.java.JavaPlugin

class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        DaisyMenu.initialize(this)
    }

    override fun onDisable() {
        DaisyMenu.shutdown()
    }
}
```

## Quick Start

```kotlin
import cat.daisy.menu.menu
import cat.daisy.menu.openMenu
import org.bukkit.Material
import org.bukkit.entity.Player

fun openSkyblockMenu(player: Player) {
    player.openMenu(
        menu("Skyblock Menu", rows = 3) {
            fill { name = " " }

            slot(13) {
                item = cat.daisy.menu.item(Material.GRASS_BLOCK) {
                    name = "Your Island"
                }

                onClick { viewer, _ ->
                    viewer.sendPlainMessage("Open island menu")
                }
            }
        },
    )
}
```

## Usage Patterns

### Static layout

```kotlin
val shopMenu =
    menu("Shop", rows = 3) {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE) {
            name = " "
        }

        slot(11) {
            item(Material.DIAMOND) {
                name = "&bDiamond"
                lore("&7Price: &a100")
            }
        }

        slot(15) {
            item(Material.BARRIER) {
                name = "&cClose"
            }

            val closeHandler: suspend MenuClickContext.() -> Unit = {
                close()
            }
            onClick(closeHandler)
        }
    }
```

### Dynamic content with `invalidate()`

```kotlin
fun openCounter(player: Player) {
    var counter = 0
    var session: MenuSession? = null

    session =
        player.openMenu(
            menu("Counter", rows = 1) {
                slot(4) {
                    render {
                        cat.daisy.menu.item(Material.PAPER) {
                            name = "Count: $counter"
                        }
                    }
                }

                slot(6) {
                    item(Material.EMERALD) {
                        name = "Increment"
                    }

                    onClick { _, _ ->
                        counter++
                        session?.invalidate(4)
                    }
                }
            },
        )
}
```

### Session-level refresh loop

```kotlin
fun openStatus(player: Player) {
    val session =
        player.openMenu(
            menu("Server Status", rows = 1) {
                slot(4) {
                    render {
                        cat.daisy.menu.item(Material.CLOCK) {
                            name = "Players: ${player.server.onlinePlayers.size}"
                        }
                    }
                }
            },
        )

    session.refreshEvery(20) {
        invalidate(4)
    }
}
```

### Slot-level refresh loop

```kotlin
player.openMenu(
    menu("Clock", rows = 1) {
        slot(4) {
            render {
                cat.daisy.menu.item(Material.CLOCK) {
                    name = "Tick: ${player.server.currentTick}"
                }
            }
            refreshEvery(20)
        }
    },
)
```

### Pagination

```kotlin
fun openPagedList(player: Player, values: List<String>) {
    player.openMenu(
        menu("Paged List", rows = 6) {
            fill { name = " " }

            pagination(itemsPerPage = 45) {
                val pageValues = pageItems(values)
                pageCount(values.size)

                pageValues.forEachIndexed { index, value ->
                    slot(index) {
                        item(Material.PAPER) {
                            name = value
                        }
                    }
                }

                if (hasPrevious()) {
                    slot(45) {
                        item(Material.ARROW) { name = "Previous" }
                        val previousHandler: suspend MenuClickContext.() -> Unit = {
                            previousPage()
                        }
                        onClick(previousHandler)
                    }
                }

                if (hasNext()) {
                    slot(53) {
                        item(Material.ARROW) { name = "Next" }
                        val nextHandler: suspend MenuClickContext.() -> Unit = {
                            nextPage()
                        }
                        onClick(nextHandler)
                    }
                }
            }
        },
    )
}
```

### Lifecycle hooks

```kotlin
player.openMenu(
    menu("Lifecycle", rows = 1) {
        onOpen { session ->
            session.player.sendPlainMessage("Menu opened")
        }

        onClose { session ->
            session.player.sendPlainMessage("Menu closed")
        }
    },
)
```

### Anvil input

```kotlin
import cat.daisy.menu.openAnvil

suspend fun askForName(player: Player) {
    val result = player.openAnvil(
        title = "&eRename Item",
        placeholder = "&7Enter a name",
    )

    if (result != null) {
        player.sendPlainMessage("You entered: $result")
    }
}
```

## API Overview

### Build menus

```kotlin
fun menu(title: String, rows: Int = 3, block: MenuBuilder.() -> Unit): Menu
fun Player.openMenu(menu: Menu): MenuSession
fun Player.openMenu(title: String, rows: Int = 3, block: MenuBuilder.() -> Unit): MenuSession
```

### `MenuSession`

```kotlin
val player: Player
val menu: Menu
val currentPage: Int

fun close()
fun invalidate()
fun invalidate(slot: Int)
fun refreshEvery(ticks: Long, block: suspend MenuSession.() -> Unit): Cancellable
```

### `SlotBuilder`

```kotlin
var item: ItemStack?

fun item(material: Material, block: ItemBuilder.() -> Unit = {})
fun render(block: MenuRenderContext.() -> ItemStack)
fun refreshEvery(ticks: Long)
fun onClick(handler: suspend MenuClickContext.() -> Unit)
fun onClick(handler: suspend (Player) -> Unit)
fun onClick(handler: suspend (Player, ClickType) -> Unit)
```

### `PaginationScope`

```kotlin
val currentPage: Int

fun pageCount(totalItems: Int): Int
fun pageRange(totalItems: Int): IntRange
fun <T> pageItems(items: List<T>): List<T>
fun hasPrevious(): Boolean
fun hasNext(): Boolean
suspend fun previousPage()
suspend fun nextPage()
```

## Safety Notes

DaisyMenu treats the top inventory as menu-owned and read-only.

- clicks in the top inventory are cancelled before handlers run
- drags touching the top inventory are cancelled
- shift-click / collect-to-cursor style transfers into the menu are blocked
- bottom-inventory clicks that stay in the player inventory remain allowed

This keeps the menu layer simple: layout and behavior live in the DSL, inventory mutation stays under DaisyMenu control.

## Testing

The library includes automated coverage for:

- builder validation
- reusable menu definitions with independent sessions
- cloned item safety
- click routing and lifecycle order
- bottom-inventory interaction rules
- drag cancellation
- pagination changes and page clamping
- refresh-task cleanup
- dynamic slot invalidation

Run the suite with:

```powershell
.\gradlew.bat test
```
