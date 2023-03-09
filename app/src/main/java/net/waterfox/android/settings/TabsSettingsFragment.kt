/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.waterfox.android.R
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.showToolbar

/**
 * Lets the user customize auto closing tabs.
 */
class TabsSettingsFragment : Fragment() {

    lateinit var view: TabsSettingsComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        view = TabsSettingsComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_tabs))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onCloseTabsNeverClick = ::enableInactiveTabsSetting
        view.onCloseTabsAfterOneDayClick = ::disableInactiveTabsSetting
        view.onCloseTabsAfterOneWeekClick = ::disableInactiveTabsSetting
        view.onCloseTabsAfterOneMonthClick = ::enableInactiveTabsSetting
        view.inactiveTabsCategoryEnabled =
            !(view.context.settings().closeTabsAfterOneDay || view.context.settings().closeTabsAfterOneWeek)
    }

    private fun enableInactiveTabsSetting() {
        view.inactiveTabsCategoryEnabled = true
    }

    private fun disableInactiveTabsSetting() {
        view.context.settings().inactiveTabsAreEnabled = false
        view.inactiveTabsCategoryEnabled = false
    }

}
