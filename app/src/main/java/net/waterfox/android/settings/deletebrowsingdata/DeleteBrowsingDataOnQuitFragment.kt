/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.deletebrowsingdata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.waterfox.android.R
import net.waterfox.android.ext.readBooleanPreference
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.ext.writeBooleanPreference

class DeleteBrowsingDataOnQuitFragment : Fragment() {

    lateinit var view: DeleteBrowsingDataOnQuitComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = DeleteBrowsingDataOnQuitComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_delete_browsing_data_on_quit))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onDeleteBrowsingDataOnQuitChange = { checked ->
            setAllCheckboxes(checked)
            view.checkboxesEnabled = checked
        }
        view.onTabsChange = ::checkboxUpdater
        view.onHistoryChange = ::checkboxUpdater
        view.onCookiesChange = ::checkboxUpdater
        view.onCacheChange = ::checkboxUpdater
        view.onPermissionsChange = ::checkboxUpdater
        view.onDownloadsChange = ::checkboxUpdater

        view.checkboxesEnabled = view.context.readBooleanPreference(
            getString(R.string.pref_key_delete_browsing_data_on_quit),
            false
        )
    }

    private fun setAllCheckboxes(checked: Boolean) {
        val context = view.context
        DeleteBrowsingDataOnQuitType.values().forEach { type ->
            context.writeBooleanPreference(type.getPreferenceKey(context), checked)
            context.settings().setDeleteDataOnQuit(type, checked)
        }
    }

    private fun checkboxUpdater(@Suppress("UNUSED_PARAMETER") checked: Boolean) {
        val settings = view.context.settings()
        if (!settings.shouldDeleteAnyDataOnQuit()) {
            view.context.writeBooleanPreference(
                getString(R.string.pref_key_delete_browsing_data_on_quit),
                false
            )
            settings.shouldDeleteBrowsingDataOnQuit = false
            // TODO: refresh
            view.checkboxesEnabled = false
        }
    }

}
