/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.waterfox.android.R
import net.waterfox.android.ext.nav
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.showToolbar

/**
 * Lets the user toggle telemetry on/off.
 */
class DataChoicesFragment : PreferenceFragmentCompat() {

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_data_collection))
        updateStudiesSection()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.data_choices_preferences, rootKey)
    }

    private fun updateStudiesSection() {
        val studiesPreference = requirePreference<Preference>(R.string.pref_key_studies_section)
        val settings = requireContext().settings()
        val stringId = if (settings.isExperimentationEnabled) {
            R.string.studies_on
        } else {
            R.string.studies_off
        }
        studiesPreference.summary = getString(stringId)

        studiesPreference.setOnPreferenceClickListener {
            val action = DataChoicesFragmentDirections.actionDataChoicesFragmentToStudiesFragment()
            view?.findNavController()?.nav(R.id.dataChoicesFragment, action)
            true
        }
    }
}
