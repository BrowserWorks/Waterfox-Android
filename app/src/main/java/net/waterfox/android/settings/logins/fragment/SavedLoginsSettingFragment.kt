/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.logins.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.waterfox.android.R
import net.waterfox.android.ext.components
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.settings.RadioButtonPreference
import net.waterfox.android.settings.SharedPreferenceUpdater
import net.waterfox.android.settings.requirePreference
import net.waterfox.android.utils.view.addToRadioGroup

class SavedLoginsSettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.save_logins_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_passwords_save_logins))
        val save = bindSave()
        val neverSave = bindNeverSave()
        addToRadioGroup(save, neverSave)
    }

    private fun bindSave(): RadioButtonPreference {
        val preferenceSave = requirePreference<RadioButtonPreference>(R.string.pref_key_save_logins)
        preferenceSave.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                // We want to reload the current session here so we can try to fill the current page
                context?.components?.useCases?.sessionUseCases?.reload?.invoke()
                return super.onPreferenceChange(preference, newValue)
            }
        }
        return preferenceSave
    }

    private fun bindNeverSave(): RadioButtonPreference {
        val preferenceNeverSave = requirePreference<RadioButtonPreference>(R.string.pref_key_never_save_logins)
        preferenceNeverSave.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                // We want to reload the current session here so we don't save any currently inserted login
                context?.components?.useCases?.sessionUseCases?.reload?.invoke()
                return super.onPreferenceChange(preference, newValue)
            }
        }
        return preferenceNeverSave
    }
}
