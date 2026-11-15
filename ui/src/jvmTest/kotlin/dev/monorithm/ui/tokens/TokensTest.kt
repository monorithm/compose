package dev.monorithm.ui.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

// The test reads the pinned IR in the submodule language/. The build gives its folder in the property monorithm.language.
class TokensTest {
    private val source = File(System.getProperty("monorithm.language"))

    private fun json(file: File): JsonObject = Json.parseToJsonElement(file.readText()).jsonObject

    private val version = json(source.resolve("package.json"))["version"]!!.jsonPrimitive.content
    private val canonical = json(source.resolve("ir/$version/canonical.json"))

    private fun token(path: String): JsonObject = canonical[path]!!.jsonObject

    private fun values(path: String): JsonObject = token(path)["values"]!!.jsonObject

    private fun color(c: JsonObject): Color {
        val rgb = c["hex"]!!.jsonPrimitive.content.substring(1).toInt(16)
        return Color(
            red = (rgb shr 16 and 0xFF) / 255f,
            green = (rgb shr 8 and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f,
            alpha = c["alpha"]!!.jsonPrimitive.double.toFloat(),
        )
    }

    private fun px(d: JsonObject) = d["value"]!!.jsonPrimitive.double.dp

    @Test
    fun `ir - the generated tokens have the values of the pinned IR`() {
        assertEquals(version, LANGUAGE)

        // A color with no axis.
        assertEquals(color(token("color.amber.400")["value"]!!.jsonObject), MonoColors.amber400)

        // A color of the axis theme, also with an alpha less than 1.
        val ring = values("color.avatar.ring")
        assertEquals(color(ring["theme=dark"]!!.jsonObject), MonoColors.avatarRing.dark)
        assertEquals(color(ring["theme=light"]!!.jsonObject), MonoColors.avatarRing.of(MonoTheme.Light))

        // A dimension of the axis density.
        val control = values("size.control-md")
        assertEquals(px(control["density=pointer"]!!.jsonObject), MonoDimensions.sizeControlMd.pointer)
        assertEquals(px(control["density=touch"]!!.jsonObject), MonoDimensions.sizeControlMd.of(MonoDensity.Touch))

        // A duration and a number.
        val press = token("duration.press")["value"]!!.jsonObject["value"]!!.jsonPrimitive.long
        assertEquals(press.milliseconds, MonoDurations.press)
        assertEquals(token("opacity.disabled")["value"]!!.jsonPrimitive.double.toFloat(), MonoNumbers.opacityDisabled)
    }
}
