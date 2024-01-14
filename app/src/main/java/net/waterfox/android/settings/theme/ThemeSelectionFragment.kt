/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        view.lightThemes.run {
            clear()
            addAll(themeManager.getLightThemes())
        }
        view.darkThemes.run {
            clear()
            addAll(themeManager.getDarkThemes())
        }
        view.onThemeSelectedClick = { theme ->
            if (theme.isLight) {
                requireComponents.settings.lightTheme = theme.id
            } else {
                requireComponents.settings.darkTheme = theme.id
            }
            activity?.recreate()
        }
    }
}
