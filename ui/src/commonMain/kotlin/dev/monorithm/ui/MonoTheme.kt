package dev.monorithm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** The tokens that the nearest MonoTheme sets. Null if no MonoTheme is above the composition. */
internal val LocalMonoTokens = staticCompositionLocalOf<MonoTokens?> { null }

/** The density of the platform: touch on Android and iOS, and pointer on the desktop. */
internal expect fun platformDensity(): MonoDensity

/** The tokens of the current composition. With no MonoTheme above, the settings of the system apply. */
@Composable
@ReadOnlyComposable
internal fun currentMonoTokens(): MonoTokens =
    LocalMonoTokens.current ?: MonoTokens(darkTheme = isSystemInDarkTheme(), density = platformDensity())

/**
 * Gives the tokens of the language to [content]. Read them with [MonoTheme], for example `MonoTheme.color.accent`.
 *
 * Each axis that you do not set keeps the context above: the MonoTheme above, or the setting of the system. With no
 * MonoTheme above, the theme is the dark setting of the system, and the density is touch on Android and iOS, and
 * pointer on the desktop.
 *
 * @param darkTheme the context of the axis theme: true for dark, false for light.
 * @param density the context of the axis density.
 */
@Composable
public fun MonoTheme(
    darkTheme: Boolean = currentMonoTokens().darkTheme,
    density: MonoDensity = currentMonoTokens().density,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMonoTokens provides MonoTokens(darkTheme, density), content = content)
}
