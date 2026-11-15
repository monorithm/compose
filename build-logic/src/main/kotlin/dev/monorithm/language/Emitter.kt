// The emitter of dev.monorithm:ui. It reads only the intermediate form (IR) of the language: ir/<version>/ of the
// submodule design/ (monorithm/design at a tag), at the version of that checkout. It writes one Kotlin file of tokens.
//
// The file has one class for each group of tokens: the first part of the token path (color, size, radius, and the
// other groups). A group that changes with an axis has one instance for each context of the axis, for example
// MonoColor.Dark and MonoColor.Light. MonoTokens selects the instances for one context of each axis, and the object
// MonoTheme gives the tokens of the current composition.
//
// The axes: the axis theme is the Boolean darkTheme, as in Compose. The axis density is the enum MonoDensity. The
// emitter stops with an error for a different axis.
//
// This emitter is a stub. It writes the tokens of four types: color, dimension, duration, and number. The other types
// (cubicBezier, fontWeight, shadow, transition) come with the components that use them.
package dev.monorithm.language

import groovy.json.JsonSlurper
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

internal object Emitter {
    @Suppress("UNCHECKED_CAST")
    private fun json(file: File): Map<String, Any?> = JsonSlurper().parse(file) as Map<String, Any?>

    /** The language version: the version of the checked-out language source. */
    fun version(source: File): String = json(source.resolve("package.json"))["version"] as String

    /** The IR of that version. */
    fun irDir(source: File): File = source.resolve("ir/${version(source)}")

    /** Stops with an error if a file of the IR does not match its checksum in the manifest, or if the versions differ. */
    fun verify(source: File) {
        val dir = irDir(source)
        val manifest = json(dir.resolve("manifest.json"))
        check(manifest["language"] == version(source)) {
            "The IR is language ${manifest["language"]}, the source is ${version(source)}"
        }
        @Suppress("UNCHECKED_CAST")
        for ((name, sum) in manifest["files"] as Map<String, String>) {
            val actual = MessageDigest.getInstance("SHA-256").digest(dir.resolve(name).readBytes())
                .joinToString("") { "%02x".format(it) }
            check(actual == sum) { "The IR file $name does not match its checksum in the manifest" }
        }
    }

    /** The types of the stub. */
    private val types = setOf("color", "dimension", "duration", "number")

    /** The Kotlin words that cannot be a member name. */
    private val reserved = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
        "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var",
        "when", "while", "tokens",
    )

    private fun words(parts: List<String>): List<String> = parts.flatMap { it.split('-') }

    private fun pascal(words: List<String>): String =
        words.joinToString("") { w -> w.replaceFirstChar { it.uppercaseChar() } }

    private fun camel(words: List<String>): String = words.first() + pascal(words.drop(1))

    /** The group of a token: the first part of its path, for example font-size. */
    fun groupOf(path: String): String = path.substringBefore('.')

    /** The name of the class of a group: font-size gives MonoFontSize. */
    fun groupClass(group: String): String = "Mono${pascal(words(listOf(group)))}"

    /** The name of the property of a group: font-size gives fontSize. */
    fun groupProperty(group: String): String = camel(words(listOf(group)))

    /**
     * The name of a token in its group, in lowerCamelCase: size.control-md gives controlMd. In the group color, the
     * colors of the roles have no prefix: color.role.accent-fg gives accentFg. A name that starts with a digit gets
     * the prefix x: space.4 gives x4.
     */
    fun memberName(path: String): String {
        var parts = path.split('.').drop(1)
        if (groupOf(path) == "color" && parts.first() == "role") parts = parts.drop(1)
        val name = camel(words(parts))
        return if (name.first().isDigit()) "x$name" else name
    }

    /** A Kotlin literal of a number: an Int if the value has no fraction. */
    private fun number(v: Number): String {
        val d = v.toDouble()
        return if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
    }

    /** A color of the IR. Compose keeps an sRGB color in 8 bits for each channel, thus the alpha is a byte. */
    private fun color(c: Map<*, *>): String {
        val hex = (c["hex"] as String).substring(1).uppercase()
        val alpha = ((c["alpha"] as Number).toDouble() * 255).roundToInt()
        return "Color(0x%02X%s)".format(alpha, hex)
    }

    /** The Kotlin type and the Kotlin expression of a plain value of the four types of the stub. */
    private fun value(type: String, v: Any?): Pair<String, String> = when (type) {
        "color" -> "Color" to color(v as Map<*, *>)
        "dimension" -> {
            val d = v as Map<*, *>
            check(d["unit"] == "px") { "The emitter knows only px, not ${d["unit"]}" }
            "Dp" to "${number(d["value"] as Number)}.dp"
        }
        "duration" -> {
            val d = v as Map<*, *>
            check(d["unit"] == "ms") { "The emitter knows only ms, not ${d["unit"]}" }
            "Duration" to "${number(d["value"] as Number)}.milliseconds"
        }
        "number" -> "Float" to "${(v as Number).toFloat()}f"
        else -> error("The stub does not emit the type $type")
    }

    /** An axis in Kotlin: the type and the property of MonoTokens, and the Kotlin value of a context. */
    private class Axis(val contexts: List<String>, val fallback: String, val type: String, val property: String, val value: (String) -> String)

    private fun axis(name: String, contexts: List<String>, fallback: String): Axis = when (name) {
        "theme" -> {
            check(contexts.toSet() == setOf("dark", "light")) { "darkTheme has no context ${contexts.joinToString()}" }
            Axis(contexts, fallback, "Boolean", "darkTheme") { if (it == "dark") "true" else "false" }
        }
        "density" -> Axis(contexts, fallback, "MonoDensity", "density") { "MonoDensity.${pascal(listOf(it))}" }
        else -> error("The emitter does not know the axis $name")
    }

    /** One token of a group. A token with no axis has one value with the key "". */
    private class Token(val path: String, val name: String, val type: String, val description: String?, val axis: String?, val values: Map<String, String>)

    /** The text of the generated file. */
    @Suppress("UNCHECKED_CAST")
    fun emit(source: File, packageName: String): String {
        val dir = irDir(source)
        val canonical = json(dir.resolve("canonical.json")) as Map<String, Map<String, Any?>>
        val axes = (json(dir.resolve("axes.json")) as Map<String, Map<String, Any?>>).mapValues { (name, a) ->
            axis(name, a["contexts"] as List<String>, a["default"] as String)
        }

        // The tokens of the four types, by group, in the order of the IR.
        val groups = linkedMapOf<String, MutableList<Token>>()
        for ((path, e) in canonical) {
            val type = e["type"] as String
            if (type !in types) continue
            val tokenAxes = e["axes"] as List<String>? ?: emptyList()
            check(tokenAxes.size <= 1) {
                "$path changes with ${tokenAxes.joinToString(" and ")}: the emitter knows one axis for each token"
            }
            // A key of values is <axis>=<context>, for example theme=dark.
            val values = if (tokenAxes.isEmpty()) {
                mapOf("" to value(type, e["value"]))
            } else {
                (e["values"] as Map<String, Any?>).entries.associate { (k, v) -> k.substringAfter('=') to value(type, v) }
            }
            val name = memberName(path)
            check(name !in reserved) { "The name $name of $path is a word of Kotlin" }
            groups.getOrPut(groupOf(path)) { mutableListOf() } += Token(
                path = path,
                name = name,
                type = values.values.first().first,
                description = (e["description"] as String?)?.trim()?.replace(Regex("\\s+"), " "),
                axis = tokenAxes.singleOrNull(),
                values = values.mapValues { it.value.second },
            )
        }

        // The axis of each group. A group has a maximum of one axis.
        val groupAxis = groups.mapValues { (group, tokens) ->
            val used = tokens.mapNotNull { it.axis }.toSet()
            check(used.size <= 1) {
                "The group $group changes with ${used.joinToString(" and ")}: the emitter knows one axis for each group"
            }
            check(tokens.map { it.name }.toSet().size == tokens.size) { "Two tokens of the group $group have the same name" }
            used.firstOrNull()
        }

        val language = json(dir.resolve("manifest.json"))["language"] as String
        val out = StringBuilder()
        fun line(s: String = "") = out.append(s).append('\n')

        line("// Generated by the task emitTokens from design/ir/$language/. Do not edit: change the language source.")
        line()
        line("package $packageName")
        line()
        line("import androidx.compose.runtime.Composable")
        line("import androidx.compose.runtime.Immutable")
        line("import androidx.compose.runtime.ReadOnlyComposable")
        line("import androidx.compose.ui.graphics.Color")
        line("import androidx.compose.ui.unit.Dp")
        line("import androidx.compose.ui.unit.dp")
        line("import kotlin.time.Duration")
        line("import kotlin.time.Duration.Companion.milliseconds")
        line()
        line("/** The language version of these tokens. */")
        line("public const val LANGUAGE: String = \"$language\"")
        line()
        val density = axes.getValue("density")
        line("/** The contexts of the axis density. The fallback is ${pascal(listOf(density.fallback))}. */")
        line("public enum class MonoDensity { ${density.contexts.joinToString(", ") { pascal(listOf(it)) }} }")

        // The expression that selects the instance of a group for the context of each axis.
        fun select(group: String): String {
            val cls = groupClass(group)
            val a = groupAxis[group]?.let { axes.getValue(it) } ?: return "$cls.Standard"
            return if (a.property == "darkTheme") {
                "if (darkTheme) $cls.Dark else $cls.Light"
            } else {
                "when (${a.property}) { ${a.contexts.joinToString("; ") { "${a.value(it)} -> $cls.${pascal(listOf(it))}" }} }"
            }
        }
        fun about(group: String): String = groupAxis[group]?.let { "The tokens $group.* for the context of the axis $it." }
            ?: "The tokens $group.*. They do not change with an axis."

        // MonoTokens: the instances of the groups for one context of each axis.
        line()
        line("/**")
        line(" * The tokens for one context of each axis: the theme ([darkTheme]) and the [density].")
        line(" *")
        line(" * Get the tokens of the current composition with [MonoTheme], for example `MonoTheme.color.accent`.")
        line(" */")
        line("@Immutable")
        line("public class MonoTokens(")
        for ((name, a) in axes) {
            line("    /** The context of the axis $name. The fallback is ${a.fallback}. */")
            line("    public val ${a.property}: ${a.type} = ${a.value(a.fallback)},")
        }
        line(") {")
        for (group in groups.keys) {
            line("    /** ${about(group)} */")
            line("    public val ${groupProperty(group)}: ${groupClass(group)} get() = ${select(group)}")
            line()
        }
        line("    override fun equals(other: Any?): Boolean =")
        line("        other is MonoTokens && ${axes.values.joinToString(" && ") { "other.${it.property} == ${it.property}" }}")
        line()
        line("    override fun hashCode(): Int = ${axes.values.joinToString(" * 31 + ") { "${it.property}.hashCode()" }}")
        line("}")

        // MonoTheme: the tokens of the current composition. The composable function MonoTheme sets the axes.
        line()
        line("/** The tokens of the language for the current composition. The composable function MonoTheme sets the axes. */")
        line("public object MonoTheme {")
        line("    /** The tokens for the context of each axis. */")
        line("    public val tokens: MonoTokens")
        line("        @Composable @ReadOnlyComposable get() = currentMonoTokens()")
        for (group in groups.keys) {
            line()
            line("    /** ${about(group)} */")
            line("    public val ${groupProperty(group)}: ${groupClass(group)}")
            line("        @Composable @ReadOnlyComposable get() = currentMonoTokens().${groupProperty(group)}")
        }
        line("}")

        // One class for each group, with one instance for each context of its axis.
        for ((group, tokens) in groups) {
            val cls = groupClass(group)
            val axis = groupAxis[group]
            val instances = axis?.let { axes.getValue(it).contexts } ?: listOf("standard")
            line()
            line(
                if (axis == null) "/** The tokens $group.*. They do not change with an axis. */"
                else "/** The tokens $group.*, with one instance for each context of the axis $axis. */",
            )
            line("@Immutable")
            line("public class $cls private constructor(")
            for (t in tokens) {
                line(if (t.description == null) "    /** The token ${t.path}. */" else "    /** ${t.path}: ${t.description} */")
                line("    public val ${t.name}: ${t.type},")
            }
            line(") {")
            line("    public companion object {")
            for ((i, instance) in instances.withIndex()) {
                if (i > 0) line()
                line(
                    if (axis == null) "        /** The values of the tokens. */"
                    else "        /** The values for the context $instance of the axis $axis. */",
                )
                val args = tokens.joinToString(", ") { t -> "${t.name} = ${t.values.getValue(if (t.axis == null) "" else instance)}" }
                line("        public val ${pascal(listOf(instance))}: $cls = $cls($args)")
            }
            line("    }")
            line("}")
        }
        return out.toString()
    }
}
