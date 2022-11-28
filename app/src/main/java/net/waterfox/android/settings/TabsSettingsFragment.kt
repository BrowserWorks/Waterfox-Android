/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import net.waterfox.android.R
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.utils.view.addToRadioGroup

/**
 * Lets the user customize auto closing tabs.
 */
class TabsSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var listRadioButton: RadioButtonPreference
    private lateinit var gridRadioButton: RadioButtonPreference
    private lateinit var radioManual: RadioButtonPreference
    private lateinit var radioOneDay: RadioButtonPreference
    private lateinit var radioOneWeek: RadioButtonPreference
    private lateinit var radioOneMonth: RadioButtonPreference
    private lateinit var inactiveTabsCategory: PreferenceCategory
    private lateinit var inactiveTabs: SwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tabs_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_tabs))

        setupPreferences()
    }

    private fun setupPreferences() {
        // This should be the only use case for pref_key_tab_view_list_do_not_use.
        // In the Waterfox impl of RadioGroups, we need to always have an individual pref value
        // for it to work. This is weird for a radio group that should hold a value from that group.
        // For the tabs tray, we only need a boolean value, so let's rely on only the
        // pref_key_tab_view_grid and look into using the native RadioGroup in the future.
        listRadioButton = requirePreference(R.string.pref_key_tab_view_list_do_not_use)
        gridRadioButton = requirePreference(R.string.pref_key_tab_view_grid)

        radioManual = requirePreference(R.string.pref_key_close_tabs_manually)
        radioOneMonth = requirePreference(R.string.pref_key_close_tabs_after_one_month)
        radioOneWeek = requirePreference(R.string.pref_key_close_tabs_after_one_week)
        radioOneDay = requirePreference(R.string.pref_key_close_tabs_after_one_day)

        inactiveTabs = requirePreference<SwitchPreference>(R.string.pref_key_inactive_tabs).also {
            it.isChecked = requireContext().settings().inactiveTabsAreEnabled
            it.onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        inactiveTabsCategory = requirePreference<PreferenceCategory>(R.string.pref_key_inactive_tabs_category).also {
            it.isEnabled = !(it.context.settings().closeTabsAfterOneDay || it.context.settings().closeTabsAfterOneWeek)
        }

        radioManual.onClickListener(::enableInactiveTabsSetting)
        radioOneDay.onClickListener(::disableInactiveTabsSetting)
        radioOneWeek.onClickListener(::disableInactiveTabsSetting)
        radioOneMonth.onClickListener(::enableInactiveTabsSetting)

        setupRadioGroups()
    }

    private fun setupRadioGroups() {
        addToRadioGroup(
            listRadioButton,
            gridRadioButton
        )

        addToRadioGroup(
            radioManual,
            radioOneDay,
            radioOneMonth,
            radioOneWeek
        )
    }

    private fun enableInactiveTabsSetting() {
        inactiveTabsCategory.apply {
            isEnabled = true
        }
    }

    private fun disableInactiveTabsSetting() {
        inactiveTabsCategory.apply {
            isEnabled = false
            inactiveTabs.isChecked = false
            context.settings().inactiveTabsAreEnabled = false
        }
    }
}
