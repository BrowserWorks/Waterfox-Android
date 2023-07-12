/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.components.PrivateShortcutCreateManager
import net.waterfox.android.ext.showToolbar

/**
 * Lets the user customize Private browsing options.
 */
class PrivateBrowsingFragment : Fragment() {

    lateinit var view: PrivateBrowsingComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        view = PrivateBrowsingComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_private_browsing_options))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onAddShortcutClick = {
            PrivateShortcutCreateManager.createPrivateShortcut(requireContext())
        }
        view.onAllowScreenshotsChange = { allow ->
            if ((activity as? HomeActivity)?.browsingModeManager?.mode?.isPrivate == true && !allow) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

}
