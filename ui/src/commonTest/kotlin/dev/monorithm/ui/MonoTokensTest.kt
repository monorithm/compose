package dev.monorithm.ui

import kotlin.test.Test
import kotlin.test.assertSame

class MonoTokensTest {
    @Test
    fun `axes - MonoTokens selects the instances of each context`() {
        val tokens = MonoTokens(darkTheme = false, density = MonoDensity.Touch)
        assertSame(MonoColor.Light, tokens.color)
        assertSame(MonoSize.Touch, tokens.size)
        assertSame(MonoDuration.Standard, tokens.duration)

        // The fallbacks of the axes of the IR: dark and pointer.
        assertSame(MonoColor.Dark, MonoTokens().color)
        assertSame(MonoSize.Pointer, MonoTokens().size)
    }
}
