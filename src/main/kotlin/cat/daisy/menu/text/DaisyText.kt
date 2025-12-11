package cat.daisy.menu.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * DaisyText - MiniMessage text utilities for DaisyMenu
 *
 * Features:
 * - MiniMessage parsing with legacy color support
 * - Gradient and rainbow text
 * - Placeholder replacement
 * - Console logging with colors
 */
public object DaisyText {
    private val miniMessage = MiniMessage.miniMessage()
    private val logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val legacyColorRegex = """&([0-9a-fk-or])""".toRegex()
    private val legacyColorMap =
        mapOf(
            "0" to "black",
            "1" to "dark_blue",
            "2" to "dark_green",
            "3" to "dark_aqua",
            "4" to "dark_red",
            "5" to "dark_purple",
            "6" to "gold",
            "7" to "gray",
            "8" to "dark_gray",
            "9" to "blue",
            "a" to "green",
            "b" to "aqua",
            "c" to "red",
            "d" to "light_purple",
            "e" to "yellow",
            "f" to "white",
        )
    private val legacyFormattingMap =
        mapOf(
            "k" to "obfuscated",
            "l" to "bold",
            "m" to "strikethrough",
            "n" to "underlined",
            "o" to "italic",
            "r" to "reset",
        )

    /**
     * Standard color palette for consistent styling.
     */
    public object Colors {
        public const val PRIMARY: String = "#3498db"
        public const val SECONDARY: String = "#2ecc71"
        public const val ERROR: String = "#e74c3c"
        public const val SUCCESS: String = "#2ecc71"
        public const val WARNING: String = "#f1c40f"
        public const val INFO: String = "#3498db"
        public const val BROADCAST: String = "#9b59b6"
        public const val SYSTEM: String = "#34495e"
        public const val ACCENT: String = "#e67e22"
        public const val MUTED: String = "#95a5a6"
    }

    /**
     * Parse string as MiniMessage with italic disabled by default.
     */
    public fun String.mm(): Component =
        miniMessage
            .deserialize(convertLegacyColors())
            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)

    /**
     * Parse string as raw MiniMessage (keeps default decorations).
     */
    public fun String.mmRaw(): Component = miniMessage.deserialize(convertLegacyColors())

    /**
     * Convert legacy &-codes to MiniMessage format.
     */
    public fun String.convertLegacyColors(): String =
        replace(legacyColorRegex) { match ->
            val code = match.groupValues[1]
            when {
                legacyColorMap.containsKey(code) -> {
                    "<${legacyColorMap[code]}>"
                }

                legacyFormattingMap.containsKey(code) -> {
                    if (code == "r") "<reset><white>" else "<${legacyFormattingMap[code]}>"
                }

                else -> {
                    match.value
                }
            }
        }

    /**
     * Apply gradient colors to text.
     */
    public fun String.gradient(vararg colors: String): Component = "<gradient:${colors.joinToString(":")}>$this</gradient>".mm()

    /**
     * Apply rainbow gradient to text.
     */
    public fun String.rainbow(): Component = gradient("#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#4B0082", "#9400D3")

    /**
     * Strip all MiniMessage tags and legacy colors.
     */
    public fun String.stripColors(): String = MiniMessage.miniMessage().stripTags(convertLegacyColors())

    /**
     * Replace placeholders in format {key} with values.
     */
    public fun String.replacePlaceholders(vararg pairs: Pair<String, Any>): String {
        var result = this
        pairs.forEach { (key, value) -> result = result.replace("{$key}", value.toString()) }
        return result
    }

    /**
     * Replace placeholders using a map.
     */
    public fun String.replacePlaceholders(placeholders: Map<String, Any>): String {
        var result = this
        placeholders.forEach { (key, value) -> result = result.replace("{$key}", value.toString()) }
        return result
    }

    /**
     * Log a message to console with timestamp and color.
     */
    public fun log(
        message: String,
        level: String = "INFO",
        throwable: Throwable? = null,
        context: Map<String, Any> = emptyMap(),
    ) {
        val timestamp = LocalDateTime.now().format(logFormatter)
        val (color, prefix) =
            when (level.uppercase()) {
                "ERROR" -> Colors.ERROR to "✖ "
                "SUCCESS" -> Colors.SUCCESS to "✔ "
                "WARNING" -> Colors.WARNING to "⚠ "
                "DEBUG" -> Colors.MUTED to "• "
                else -> Colors.INFO to "✦ "
            }
        val contextStr =
            if (context.isNotEmpty()) {
                " ${context.entries.joinToString(" | ") { "${it.key}: ${it.value}" }}"
            } else {
                ""
            }
        val fullMessage = "[$timestamp] [$level] $prefix $message$contextStr"
        Bukkit.getConsoleSender().sendMessage("<$color>$fullMessage</$color>".mm())
        throwable?.printStackTrace()
    }

    public fun logInfo(message: String): Unit = log(message, "INFO")

    public fun logSuccess(message: String): Unit = log(message, "SUCCESS")

    public fun logWarning(message: String): Unit = log(message, "WARNING")

    public fun logError(
        message: String,
        throwable: Throwable? = null,
    ): Unit = log(message, "ERROR", throwable)

    public fun logDebug(message: String): Unit = log(message, "DEBUG")
}
