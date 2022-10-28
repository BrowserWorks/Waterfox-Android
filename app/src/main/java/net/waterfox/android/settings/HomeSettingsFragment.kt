/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import net.waterfox.android.FeatureFlags
import net.waterfox.android.R
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.utils.view.addToRadioGroup

/**
 * Lets the user customize the home screen.
 */
class HomeSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_home_2))
        setupPreferences()
    }

    private fun setupPreferences() {
        requirePreference<SwitchPreference>(R.string.pref_key_show_top_sites).apply {
            isChecked = context.settings().showTopSitesFeature
        }

        requirePreference<CheckBoxPreference>(R.string.pref_key_enable_contile).apply {
            isChecked = context.settings().showContileFeature
        }

        requirePreference<SwitchPreference>(R.string.pref_key_recent_tabs).apply {
            isVisible = FeatureFlags.showRecentTabsFeature
            isChecked = context.settings().showRecentTabsFeature
        }

        requirePreference<SwitchPreference>(R.string.pref_key_recent_bookmarks).apply {
            isVisible = FeatureFlags.recentBookmarksFeature
            isChecked = context.settings().showRecentBookmarksFeature
        }

        requirePreference<SwitchPreference>(R.string.pref_key_history_metadata_feature).apply {
            isVisible = FeatureFlags.historyMetadataUIFeature
            isChecked = context.settings().historyMetadataUIFeature
        }

        val openingScreenRadioHomepage =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_always)
        val openingScreenLastTab =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_never)
        val openingScreenAfterFourHours =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_after_four_hours)

        requirePreference<Preference>(R.string.pref_key_wallpapers).apply {
            setOnPreferenceClickListener {
                view?.findNavController()?.navigate(
                    HomeSettingsFragmentDirections.actionHomeSettingsFragmentToWallpaperSettingsFragment()
                )
                true
            }
        }

        addToRadioGroup(
            openingScreenRadioHomepage,
            openingScreenLastTab,
            openingScreenAfterFourHours
        )
    }
}
