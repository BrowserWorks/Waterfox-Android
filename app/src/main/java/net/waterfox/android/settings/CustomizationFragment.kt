/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import net.waterfox.android.R
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.showToolbar

/**
 * Lets the user customize the UI.
 */
class CustomizationFragment : Fragment() {

    lateinit var view: CustomizationComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = CustomizationComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_customize))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onSelectThemeClick = {
            view.findNavController().navigate(
                CustomizationFragmentDirections.actionCustomizationFragmentToThemeSelectionFragment()
            )
        }
    }
}
