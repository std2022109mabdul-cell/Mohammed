package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: ThemeColorConfig = ThemeColorConfig.TEAL,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (darkTheme) {
        true -> when (themeColor) {
            ThemeColorConfig.TEAL -> darkColorScheme(primary = DarkPrimaryTeal, secondary = DarkSecondaryTeal, tertiary = DarkTertiaryTeal, background = DarkBackground, surface = DarkSurface, onPrimary = DarkOnPrimary, onBackground = DarkOnBackground, onSurface = DarkOnSurface)
            ThemeColorConfig.ORANGE -> darkColorScheme(primary = DarkPrimaryOrange, secondary = DarkSecondaryOrange, tertiary = DarkTertiaryOrange, background = DarkBackground, surface = DarkSurface, onPrimary = DarkOnPrimary, onBackground = DarkOnBackground, onSurface = DarkOnSurface)
            ThemeColorConfig.BLUE -> darkColorScheme(primary = DarkPrimaryBlue, secondary = DarkSecondaryBlue, tertiary = DarkTertiaryBlue, background = DarkBackground, surface = DarkSurface, onPrimary = DarkOnPrimary, onBackground = DarkOnBackground, onSurface = DarkOnSurface)
            ThemeColorConfig.PURPLE -> darkColorScheme(primary = DarkPrimaryPurple, secondary = DarkSecondaryPurple, tertiary = DarkTertiaryPurple, background = DarkBackground, surface = DarkSurface, onPrimary = DarkOnPrimary, onBackground = DarkOnBackground, onSurface = DarkOnSurface)
            ThemeColorConfig.PINK -> darkColorScheme(primary = DarkPrimaryPink, secondary = DarkSecondaryPink, tertiary = DarkTertiaryPink, background = DarkBackground, surface = DarkSurface, onPrimary = DarkOnPrimary, onBackground = DarkOnBackground, onSurface = DarkOnSurface)
        }
        false -> when (themeColor) {
            ThemeColorConfig.TEAL -> lightColorScheme(primary = LightPrimaryTeal, secondary = LightSecondaryTeal, tertiary = LightTertiaryTeal, background = LightBackground, surface = LightSurface, onPrimary = LightOnPrimary, onBackground = LightOnBackground, onSurface = LightOnSurface)
            ThemeColorConfig.ORANGE -> lightColorScheme(primary = LightPrimaryOrange, secondary = LightSecondaryOrange, tertiary = LightTertiaryOrange, background = LightBackground, surface = LightSurface, onPrimary = LightOnPrimary, onBackground = LightOnBackground, onSurface = LightOnSurface)
            ThemeColorConfig.BLUE -> lightColorScheme(primary = LightPrimaryBlue, secondary = LightSecondaryBlue, tertiary = LightTertiaryBlue, background = LightBackground, surface = LightSurface, onPrimary = LightOnPrimary, onBackground = LightOnBackground, onSurface = LightOnSurface)
            ThemeColorConfig.PURPLE -> lightColorScheme(primary = LightPrimaryPurple, secondary = LightSecondaryPurple, tertiary = LightTertiaryPurple, background = LightBackground, surface = LightSurface, onPrimary = LightOnPrimary, onBackground = LightOnBackground, onSurface = LightOnSurface)
            ThemeColorConfig.PINK -> lightColorScheme(primary = LightPrimaryPink, secondary = LightSecondaryPink, tertiary = LightTertiaryPink, background = LightBackground, surface = LightSurface, onPrimary = LightOnPrimary, onBackground = LightOnBackground, onSurface = LightOnSurface)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
