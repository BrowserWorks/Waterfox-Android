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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.createWindowInsetsController
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.customtabs.ExternalAppBrowserActivity
import net.waterfox.android.ext.settings

const val THEME_LIGHT_DEFAULT = "light_default"
const val THEME_DARK_DEFAULT = "dark_default"

abstract class ThemeManager {

    abstract var currentTheme: BrowsingMode

    /**
     * Returns the style resource corresponding to the [currentTheme].
     */
    @StyleRes
    fun getCurrentThemeResource(context: Context) = when (currentTheme) {
        BrowsingMode.Normal -> getWaterfoxTheme(context).resourceId
        BrowsingMode.Private -> R.style.PrivateTheme
    }

    fun getLightThemes() = THEMES.filter { it.isLight }

    fun getDarkThemes() = THEMES.filterNot { it.isLight }

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

fun getWaterfoxTheme(context: Context): WaterfoxPredefinedTheme {
    val themeId = if (isDarkTheme(context)) {
        context.settings().darkTheme
    } else {
        context.settings().lightTheme
    }
    return THEMES.find { it.id == themeId }!!
}

private fun isDarkTheme(context: Context) =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

private val THEMES = listOf(
    WaterfoxPredefinedTheme(
        id = THEME_LIGHT_DEFAULT,
        name = R.string.theme_light_default,
        colors = lightColorPalette,
        resourceId = R.style.NormalTheme,
        isLight = true,
        thumbnail = R.drawable.onboarding_light_theme,
    ),
    WaterfoxPredefinedTheme(
        id = "light_grey",
        name = R.string.theme_light_grey,
        colors = lightGreyColorPalette,
        resourceId = R.style.WaterFoxLightThemeGrey,
        isLight = true,
        thumbnail = R.drawable.theme_thumbnail_light_grey,
    ),
    WaterfoxPredefinedTheme(
        id = THEME_DARK_DEFAULT,
        name = R.string.theme_dark_default,
        colors = darkColorPalette,
        resourceId = R.style.NormalTheme,
        isLight = false,
        thumbnail = R.drawable.onboarding_dark_theme,
    ),
    WaterfoxPredefinedTheme(
        id = "dark_grey",
        name = R.string.theme_dark_grey,
        colors = darkGreyColorPalette,
        resourceId = R.style.WaterFoxDarkThemeGrey,
        isLight = false,
        thumbnail = R.drawable.theme_thumbnail_dark_grey,
    ),
)
