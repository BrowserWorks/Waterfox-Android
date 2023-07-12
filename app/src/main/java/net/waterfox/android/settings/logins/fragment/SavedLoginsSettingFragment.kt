/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.logins.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.waterfox.android.R
import net.waterfox.android.ext.components
import net.waterfox.android.ext.showToolbar

class SavedLoginsSettingFragment : Fragment() {

    lateinit var view: SavedLoginsSettingComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = SavedLoginsSettingComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_passwords_save_logins))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onAskToSaveClick = {
            // We want to reload the current session here so we can try to fill the current page
            context?.components?.useCases?.sessionUseCases?.reload?.invoke()
        }
        view.onNeverSaveClick = {
            // We want to reload the current session here so we don't save any currently inserted login
            context?.components?.useCases?.sessionUseCases?.reload?.invoke()
        }
    }

}
