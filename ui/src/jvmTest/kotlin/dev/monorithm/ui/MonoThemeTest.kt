package dev.monorithm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class MonoThemeTest {
    /** Composes [content] and returns the tokens that [content] reads, with the dark setting of the system. */
    private fun tokensOf(content: @Composable (probe: @Composable () -> Unit) -> Unit): Pair<MonoTokens, Boolean> {
        lateinit var tokens: MonoTokens
        var systemDark = false
        runComposeUiTest {
            setContent {
                systemDark = isSystemInDarkTheme()
                content { tokens = MonoTheme.tokens }
            }
        }
        return tokens to systemDark
    }

    @Test
    fun `platform - with no MonoTheme, the settings of the system apply`() {
        val (tokens, systemDark) = tokensOf { probe -> probe() }
        assertEquals(systemDark, tokens.darkTheme)
        assertEquals(MonoDensity.Pointer, tokens.density)
    }

    @Test
    fun `override - a MonoTheme sets the axes of its content`() {
        val (tokens, _) = tokensOf { probe ->
            MonoTheme(darkTheme = false, density = MonoDensity.Touch) { probe() }
        }
        assertSame(MonoColor.Light, tokens.color)
        assertSame(MonoSize.Touch, tokens.size)
    }

    @Test
    fun `nested - a MonoTheme keeps the axis that it does not set`() {
        val (tokens, _) = tokensOf { probe ->
            MonoTheme(darkTheme = false, density = MonoDensity.Touch) {
                MonoTheme(density = MonoDensity.Pointer) { probe() }
            }
        }
        assertEquals(false, tokens.darkTheme)
        assertEquals(MonoDensity.Pointer, tokens.density)
    }
}
