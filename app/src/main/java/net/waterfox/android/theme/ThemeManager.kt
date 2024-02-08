/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.TypedValue
import android.view.Window
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.createWindowInsetsController
import mozilla.components.ui.colors.PhotonColors
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.customtabs.ExternalAppBrowserActivity
import net.waterfox.android.ext.settings

abstract class ThemeManager {

    abstract var currentTheme: BrowsingMode

    /**
     * Returns the style resource corresponding to the [currentTheme].
     */
    @StyleRes
    fun getCurrentThemeResource(context: Context) = when (currentTheme) {
        BrowsingMode.Normal -> getWaterfoxColorScheme(context).resourceId
        BrowsingMode.Private -> R.style.PrivateTheme
    }

    fun getColorSchemes(): List<WaterfoxThemeColorScheme> = COLOR_SCHEMES

    /**
     * Handles status bar theme change since the window does not dynamically recreate
     */
    fun applyStatusBarTheme(activity: Activity) = applyStatusBarTheme(activity.window, activity)
    fun applyStatusBarTheme(window: Window, context: Context) {
        when (currentTheme) {
            BrowsingMode.Normal -> {
                when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_UNDEFINED, // We assume light here per Android doc's recommendation
                    Configuration.UI_MODE_NIGHT_NO -> {
                        updateLightSystemBars(window, context)
                    }
                    Configuration.UI_MODE_NIGHT_YES -> {
                        clearLightSystemBars(window)
                        updateNavigationBar(window, context)
                    }
                }
            }
            BrowsingMode.Private -> {
                clearLightSystemBars(window)
                updateNavigationBar(window, context)
            }
        }
    }

    fun setActivityTheme(activity: Activity) {
        activity.setTheme(getCurrentThemeResource(activity))
    }

    companion object {
        fun resolveAttribute(attribute: Int, context: Context): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(attribute, typedValue, true)

            return typedValue.resourceId
        }

        @Composable
        fun resolveAttributeColor(attribute: Int): androidx.compose.ui.graphics.Color {
            val resourceId = resolveAttribute(attribute, LocalContext.current)
            return colorResource(resourceId)
        }

        private fun updateLightSystemBars(window: Window, context: Context) {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                window.statusBarColor = context.getColorFromAttr(android.R.attr.statusBarColor)
                window.createWindowInsetsController().isAppearanceLightStatusBars = true
            } else {
                window.statusBarColor = Color.BLACK
            }

            if (SDK_INT >= Build.VERSION_CODES.O) {
                // API level can display handle light navigation bar color
                window.createWindowInsetsController().isAppearanceLightNavigationBars = true

                updateNavigationBar(window, context)
            }
        }

        private fun clearLightSystemBars(window: Window) {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                window.createWindowInsetsController().isAppearanceLightStatusBars = false
            }

            if (SDK_INT >= Build.VERSION_CODES.O) {
                // API level can display handle light navigation bar color
                window.createWindowInsetsController().isAppearanceLightNavigationBars = false
            }
        }

        private fun updateNavigationBar(window: Window, context: Context) {
            window.navigationBarColor = context.getColorFromAttr(R.attr.layer1)
        }
    }
}

class DefaultThemeManager(
    currentTheme: BrowsingMode,
    private val activity: Activity
) : ThemeManager() {
    override var currentTheme: BrowsingMode = currentTheme
        set(value) {
            if (currentTheme != value) {
                // ExternalAppBrowserActivity doesn't need to switch between private and non-private.
                if (activity is ExternalAppBrowserActivity) return
                // Don't recreate if activity is finishing
                if (activity.isFinishing) return

                field = value

                val intent = activity.intent ?: Intent().also { activity.intent = it }
                intent.putExtra(HomeActivity.PRIVATE_BROWSING_MODE, value == BrowsingMode.Private)

                activity.recreate()
            }
        }
}

fun getWaterfoxColorScheme(context: Context): WaterfoxThemeColorScheme {
    val colorScheme = context.settings().themeColorScheme
    return COLOR_SCHEMES.find { it.id == colorScheme }!!
}

val COLOR_SCHEMES = listOf(
    WaterfoxThemeColorScheme(
        id = -1,
        lightColors = lightColorPalette,
        darkColors = darkColorPalette,
        resourceId = R.style.NormalTheme,
        lightPrimaryColor = lightColorPalette.layer1,
        darkPrimaryColor = darkColorPalette.layer1,
    ),
    WaterfoxThemeColorScheme(
        id = 0,
        lightColors = lightColorPalette,
        darkColors = darkColorPalette,
        resourceId = R.style.WaterfoxThemeRed,
        lightPrimaryColor = PhotonColors.Red50,
        darkPrimaryColor = PhotonColors.Red50,
        brush = Brush.radialGradient(
            colors = listOf(
                PhotonColors.Red50,
                PhotonColors.Red70,
            )
        ),
    ),
    WaterfoxThemeColorScheme(
        id = 1,
        lightColors = lightColorPalette,
        darkColors = darkColorPalette,
        resourceId = R.style.WaterfoxThemeGreen,
        lightPrimaryColor = PhotonColors.Green70,
        darkPrimaryColor = PhotonColors.Green70,
        brush = Brush.radialGradient(
            colors = listOf(
                PhotonColors.Green70,
                PhotonColors.Green80,
            )
        ),
    ),
    WaterfoxThemeColorScheme(
        id = 2,
        lightColors = lightColorPalette,
        darkColors = darkColorPalette,
        resourceId = R.style.WaterfoxThemeBlue,
        lightPrimaryColor = PhotonColors.Blue30,
        darkPrimaryColor = PhotonColors.Blue30,
    ),
    WaterfoxThemeColorScheme(
        id = 3,
        lightColors = lightColorPalette,
        darkColors = darkColorPalette,
        resourceId = R.style.WaterfoxThemeYellow,
        lightPrimaryColor = PhotonColors.Yellow70,
        darkPrimaryColor = PhotonColors.Yellow70,
    ),
)
