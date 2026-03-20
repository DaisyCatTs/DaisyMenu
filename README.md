# DaisyMenu

Kotlin-first menus for Paper `1.21.11` with reusable definitions, per-player sessions, strict top-inventory ownership, and a DSL that stays small even as menus get more dynamic.

[![Version](https://img.shields.io/badge/version-2.0.0-1f7a8c)](https://github.com/DaisyCatTs/DaisyMenu)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.20-7f52ff)](https://kotlinlang.org/)
[![Paper](https://img.shields.io/badge/paper-1.21.11-ffffff?logo=paper)](https://papermc.io/)
[![CI](https://img.shields.io/github/actions/workflow/status/DaisyCatTs/DaisyMenu/ci.yml?branch=main&label=build)](https://github.com/DaisyCatTs/DaisyMenu/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-MIT-0f172a)](LICENSE)

## Why DaisyMenu

DaisyMenu is built around a simple idea: the cleanest API should also be the safe one. Menus are immutable definitions, every viewer gets an isolated runtime session, dynamic content is explicit, pagination stays inside the DSL, and item transfer edge cases are blocked by default so plugin authors do not have to rebuild inventory safety from scratch.

```kotlin
val menu =
    menu("Skyblock Menu", rows = 3) {
        template {
            border { name = " " }
        }

        slot(13) {
            item(Material.GRASS_BLOCK) {
                name = "Your Island"
            }

            onClick {
                player.sendMessage("Open island menu")
            }
        }
    }

player.openMenu(menu)
```

## Features

| Feature | What it gives you |
| --- | --- |
| Immutable menu definitions | Build once and safely open for many players |
| Per-viewer sessions | Independent page state, refresh tasks, and lifecycle callbacks |
| Dynamic slot rendering | `render { ... }` with diff-based updates and targeted invalidation |
| Strict inventory ownership | Top inventory clicks, drags, and transfer-style actions are blocked before handlers run |
| Kotlin-first click DSL | `onClick`, filtered click helpers, `closeOnClick`, and `invalidateOnClick` |
| Pagination helpers | `pageItems`, `previousButton`, `nextButton`, `pageLabel`, and session page navigation |
| Template helpers | `border`, `corners`, `content`, and `navBar` for fast layout scaffolding |
| Anvil input utility | Secondary text-input helper that uses Paper's real anvil view |
| Test coverage | MockBukkit-backed tests for rendering, lifecycle, pagination, and inventory safety rules |

## Installation

### Requirements

- Java `21`
- Paper `1.21.11`
- Kotlin `2.3.20`

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.DaisyCatTs:DaisyMenu:2.0.0")
}
```

### Gradle Groovy

```groovy
repositories {
    mavenCentral()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.DaisyCatTs:DaisyMenu:2.0.0"
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
import cat.daisy.menu.item
import cat.daisy.menu.menu
import cat.daisy.menu.openMenu
import org.bukkit.Material
import org.bukkit.entity.Player

fun openSkyblockMenu(player: Player) {
    player.openMenu(
        menu("Skyblock Menu", rows = 3) {
            template {
                border { name = " " }
            }

            slot(13) {
                item = item(Material.GRASS_BLOCK) {
                    name = "Your Island"
                }

                onClick {
                    player.sendMessage("Open island menu")
                }
            }
        },
    )
}
```

## Usage Patterns

### Reusable menu definition

```kotlin
val shopMenu =
    menu("Shop", rows = 3) {
        template {
            border { name = " " }
        }

        slot(11) {
            item(Material.DIAMOND) {
                name = "&bDiamond"
                lore("&7Price: &a100 coins")
            }
        }

        slot(15) {
            item(Material.BARRIER) {
                name = "&cClose"
            }
            closeOnClick()
        }
    }

player.openMenu(shopMenu)
```

### Dynamic content with targeted invalidation

```kotlin
import cat.daisy.menu.item

fun openCounter(player: Player) {
    var counter = 0

    player.openMenu(
        menu("Counter", rows = 1) {
            slot(4) {
                render {
                    item(Material.PAPER) {
                        name = "Count: $counter"
                    }
                }
            }

            slot(6) {
                item(Material.EMERALD) {
                    name = "Increment"
                }

                onClick {
                    counter++
                    invalidate(4)
                }
            }
        },
    )
}
```

### Session refresh loop

```kotlin
import cat.daisy.menu.item

fun openStatus(player: Player) {
    val session =
        player.openMenu(
            menu("Status", rows = 1) {
                slot(4) {
                    render {
                        item(Material.CLOCK) {
                            name = "Online: ${player.server.onlinePlayers.size}"
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

### Slot refresh loop

```kotlin
import cat.daisy.menu.item

player.openMenu(
    menu("Clock", rows = 1) {
        slot(4) {
            render {
                item(Material.CLOCK) {
                    name = "Tick: ${player.server.currentTick}"
                }
            }
            refreshEvery(20)
        }
    },
)
```

### Pagination with built-in navigation helpers

```kotlin
fun openPagedList(player: Player, values: List<String>) {
    player.openMenu(
        menu("Paged List", rows = 2) {
            pagination(itemsPerPage = 7) {
                pageCount(values.size)

                pageItems(values).forEachIndexed { index, value ->
                    slot(index) {
                        item(Material.PAPER) {
                            name = value
                        }
                    }
                }

                previousButton(15)
                pageLabel(16) { currentPage, totalPages ->
                    cat.daisy.menu.item(Material.BOOK) {
                        name = "Page $currentPage/$totalPages"
                    }
                }
                nextButton(17)
            }
        },
    )
}
```

### Template helpers

```kotlin
fun openTemplateMenu(player: Player) {
    player.openMenu(
        menu("Template", rows = 6) {
            template {
                border { name = " " }
                content(10..43)
                navBar(6) {
                    previous()
                    pageLabel()
                    next()
                }
            }
        },
    )
}
```

### Click filters

```kotlin
slot(13) {
    item(Material.STONE_BUTTON) {
        name = "Actions"
    }

    onShiftClick {
        player.sendMessage("Shift click")
    }

    onLeftClick {
        player.sendMessage("Left click")
    }

    onDropClick {
        close()
    }
}
```

### Lifecycle hooks

```kotlin
player.openMenu(
    menu("Lifecycle", rows = 1) {
        onOpen { session ->
            session.player.sendMessage("Menu opened")
        }

        onClose { session ->
            session.player.sendMessage("Menu closed")
        }
    },
)
```

### Anvil input

```kotlin
import cat.daisy.menu.openAnvil

suspend fun askForName(player: Player) {
    val result =
        player.openAnvil(
            title = "&eRename Item",
            placeholder = "&7Enter a name",
        )

    if (result != null) {
        player.sendMessage("You entered: $result")
    }
}
```

## API Overview

### Core entrypoints

```kotlin
fun menu(title: String, rows: Int = 3, block: MenuBuilder.() -> Unit): Menu
fun Player.openMenu(menu: Menu): MenuSession
fun Player.openMenu(title: String, rows: Int = 3, block: MenuBuilder.() -> Unit): MenuSession
```

### `MenuSession`

```kotlin
val player: Player
val menu: Menu
val inventory: Inventory
val currentPage: Int

fun close()
fun invalidate()
fun invalidate(slot: Int)
fun invalidate(vararg slots: Int)
fun refreshEvery(ticks: Long, block: suspend MenuSession.() -> Unit): Cancellable
fun hasOpenInventory(): Boolean
```

### `SlotBuilder`

```kotlin
var item: ItemStack?

fun item(material: Material, block: ItemBuilder.() -> Unit = {})
fun render(block: MenuRenderContext.() -> ItemStack?)
fun refreshEvery(ticks: Long)

fun onClick(handler: suspend MenuClickContext.() -> Unit)
fun onPlayerClick(handler: suspend Player.() -> Unit)
fun onClick(handler: suspend (Player, ClickType) -> Unit)

fun onLeftClick(handler: suspend MenuClickContext.() -> Unit)
fun onRightClick(handler: suspend MenuClickContext.() -> Unit)
fun onShiftClick(handler: suspend MenuClickContext.() -> Unit)
fun onMiddleClick(handler: suspend MenuClickContext.() -> Unit)
fun onDropClick(handler: suspend MenuClickContext.() -> Unit)

fun closeOnClick()
fun invalidateOnClick(vararg slots: Int)
```

### `PaginationScope`

```kotlin
val currentPage: Int
val itemsPerPage: Int

fun pageCount(totalItems: Int): Int
fun pageRange(totalItems: Int): IntRange
fun <T> pageItems(items: List<T>): List<T>

fun hasPrevious(): Boolean
fun hasNext(): Boolean
suspend fun previousPage()
suspend fun nextPage()

fun previousButton(index: Int, block: SlotBuilder.() -> Unit = {})
fun nextButton(index: Int, block: SlotBuilder.() -> Unit = {})
fun pageLabel(index: Int, block: (currentPage: Int, totalPages: Int) -> ItemStack)
```

## Safety Notes

DaisyMenu treats the top inventory as menu-owned and read-only.

- Top inventory clicks are cancelled before DaisyMenu runs your handlers.
- Drags touching any top slot are cancelled.
- Transfer-style actions such as shift-click, collect-to-cursor, and hotbar move-and-readd are blocked when a menu is open.
- Bottom-inventory clicks that stay entirely inside the player's inventory remain allowed.
- Sessions do not force cursor clearing or silently drop items on close.

The goal is straightforward: plugin authors write layout and behavior, DaisyMenu owns the fragile inventory rules.

## Testing and Quality

The repository includes MockBukkit-backed tests for:

- builder validation
- reusable menu definitions with isolated sessions
- cloned item safety
- click filter ordering
- dynamic `render {}` updates and `null` clears
- inventory transfer blocking
- drag cancellation
- pagination helper behavior and page clamping
- refresh-task cleanup

Run the full verification locally with:

```powershell
.\gradlew.bat build
```

## Stability

DaisyMenu `2.0.0` targets Paper `1.21.11` and is intentionally Kotlin-first. This release is a clean API break from the earlier menu DSL in order to make the safe, modern path the easiest one to write and maintain.
