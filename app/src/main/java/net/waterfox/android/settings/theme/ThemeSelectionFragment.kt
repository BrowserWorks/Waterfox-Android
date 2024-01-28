/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.theme

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.showToolbar

class ThemeSelectionFragment : Fragment() {

    lateinit var view: ThemeSelectionComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        view = ThemeSelectionComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_select_theme))
        setupPreferences()
    }

    private fun setupPreferences() {
        val themeManager = (activity as HomeActivity).themeManager
        view.colorSchemes.run {
            clear()
            addAll(themeManager.getColorSchemes())
        }
        view.selectedColorScheme = themeManager.getColorSchemes()
            .find { it.id == requireComponents.settings.themeColorScheme}
        view.onColorSchemeSelected = { colorScheme ->
            requireComponents.settings.themeColorScheme = colorScheme.id
            activity?.recreate()
        }
        view.selectedMode = AppCompatDelegate.getDefaultNightMode()
        view.onLightThemeClick = {
            setNewTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }
        view.onDarkThemeClick = {
            setNewTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            view.onSetByBatterySaverClick = {
                setNewTheme(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.onFollowDeviceThemeClick = {
                setNewTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun setNewTheme(mode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        view.selectedMode = mode
        AppCompatDelegate.setDefaultNightMode(mode)
        activity?.recreate()
        with(requireComponents.core) {
            engine.settings.preferredColorScheme = getPreferredColorScheme()
        }
        requireComponents.useCases.sessionUseCases.reload.invoke()
    }
}
