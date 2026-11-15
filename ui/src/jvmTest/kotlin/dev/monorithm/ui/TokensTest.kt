package dev.monorithm.ui

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

// The test reads the pinned IR in the submodule design/. The build gives its folder in the property monorithm.design.
class TokensTest {
    private val source = File(System.getProperty("monorithm.design"))

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
        assertEquals(version, DESIGN_VERSION)

        // A color with no axis has the same value in each theme.
        val amber = color(token("color.amber.400")["value"]!!.jsonObject)
        assertEquals(amber, MonoColor.Dark.amber400)
        assertEquals(amber, MonoColor.Light.amber400)

        // A color of a role, of the axis theme.
        val accent = values("color.role.accent")
        assertEquals(color(accent["theme=dark"]!!.jsonObject), MonoColor.Dark.accent)
        assertEquals(color(accent["theme=light"]!!.jsonObject), MonoColor.Light.accent)

        // A color with an alpha less than 1.
        assertEquals(color(values("color.avatar.ring")["theme=light"]!!.jsonObject), MonoColor.Light.avatarRing)

        // A dimension of the axis density.
        val control = values("size.control-md")
        assertEquals(px(control["density=pointer"]!!.jsonObject), MonoSize.Pointer.controlMd)
        assertEquals(px(control["density=touch"]!!.jsonObject), MonoSize.Touch.controlMd)

        // A name that starts with a digit, a duration, and a number.
        assertEquals(px(token("space.4")["value"]!!.jsonObject), MonoSpace.Touch.x4)
        val press = token("duration.press")["value"]!!.jsonObject["value"]!!.jsonPrimitive.long
        assertEquals(press.milliseconds, MonoDuration.Standard.press)
        assertEquals(token("opacity.disabled")["value"]!!.jsonPrimitive.double.toFloat(), MonoOpacity.Standard.disabled)
    }
}
