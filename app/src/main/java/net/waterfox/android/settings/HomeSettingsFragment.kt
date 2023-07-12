/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import net.waterfox.android.R
import net.waterfox.android.ext.showToolbar

/**
 * Lets the user customize the home screen.
 */
class HomeSettingsFragment : Fragment() {

    lateinit var view: HomeSettingsComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = HomeSettingsComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_home_2))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onWallpapersClick = {
            view.findNavController().navigate(
                HomeSettingsFragmentDirections.actionHomeSettingsFragmentToWallpaperSettingsFragment()
            )
        }
    }

}
